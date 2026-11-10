"""Compare all raster values/metadata and summarize paired Java 8 benchmark runs."""
import argparse
import csv
import hashlib
import json
import statistics
from pathlib import Path

import numpy as np
import rasterio


def measurements(folder):
    with (folder / 'metrics.csv').open() as stream:
        rows = list(csv.DictReader(stream))
    assert [int(row['iteration']) for row in rows] == [0, 1, 2, 3]
    later = [float(row['elapsed_ms']) for row in rows[1:]]
    return dict(first_ms=float(rows[0]['elapsed_ms']), median_ms=statistics.median(later),
                minimum_ms=min(later), maximum_ms=max(later),
                peak_heap_bytes=max(int(row['peak_heap_bytes']) for row in rows),
                peak_heap_delta_bytes=max(int(row['heap_delta_bytes']) for row in rows)), rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('results', type=Path)
    parser.add_argument('--original', type=Path,
                        default=Path(__file__).resolve().parents[1] /
                        'docs/validation/gt17-terrain-2026-11-10/accuracy.json')
    args = parser.parse_args()
    manifest = json.loads((args.results / 'manifest.json').read_text(encoding='utf-8-sig'))
    original = json.loads(args.original.read_text(encoding='utf-8'))
    expected = {row['radius_m']: row for row in original['cases']}
    assert manifest['dem_sha256'] == original['dem_sha256'], 'DEM changed'
    with rasterio.open(manifest['dem']) as source:
        input_cells = source.width * source.height
    summary, comparisons, all_metrics = [], [], []
    for radius in (1000, 5000, 10000, 0):
        folders = {variant: args.results / variant / str(radius) for variant in ('baseline', 'optimized')}
        with rasterio.open(folders['baseline'] / 'viewshed.tif') as before, \
                rasterio.open(folders['optimized'] / 'viewshed.tif') as after:
            for attribute in ('crs', 'transform', 'dtypes', 'nodata', 'width', 'height', 'colorinterp'):
                assert getattr(before, attribute) == getattr(after, attribute), attribute
            assert before.nodata == 255
            if before.colorinterp[0] == rasterio.enums.ColorInterp.palette:
                assert before.colormap(1) == after.colormap(1), 'Palette changed'
            a, b = before.read(1), after.read(1)
            mismatches = int(np.count_nonzero(a != b))
            assert mismatches == 0, f'{radius}: {mismatches} pixels differ'
            checksum = hashlib.sha256(b.tobytes()).hexdigest()
            assert checksum == expected[radius]['sha256'], 'Original verified result changed'
            stats = {}
            for variant, folder in folders.items():
                stats[variant], raw = measurements(folder)
                assert all(row['sha256'] == checksum for row in raw), 'Non-deterministic result'
                all_metrics.extend(dict(variant=variant, **row) for row in raw)
                environment = (folder / 'environment.txt').read_text(encoding='utf-8')
                assert 'java=1.8.0_202' in environment
                if variant == 'baseline':
                    assert 'baseline-build/classes/' in environment.replace('\\', '/')
                else:
                    assert 'gt-process-spatialstatistics-17.5.jar' in environment
            baseline, optimized = stats['baseline'], stats['optimized']
            row = dict(radius_m=radius, baseline_median_ms=baseline['median_ms'],
                       optimized_median_ms=optimized['median_ms'],
                       speedup=baseline['median_ms'] / optimized['median_ms'],
                       time_reduction_percent=100 * (1 - optimized['median_ms'] / baseline['median_ms']),
                       baseline_peak_heap_MiB=baseline['peak_heap_bytes'] / 2**20,
                       optimized_peak_heap_MiB=optimized['peak_heap_bytes'] / 2**20,
                       heap_reduction_percent=100 * (1 - optimized['peak_heap_bytes'] / baseline['peak_heap_bytes']),
                       baseline_array_cells=input_cells, optimized_array_cells=int(b.size),
                       pixels_compared=int(b.size), pixel_mismatches=mismatches, sha256=checksum)
            summary.append(row)
            comparisons.append(dict(radius_m=radius, shape=list(b.shape), metadata_equal=True,
                                    baseline=baseline, optimized=optimized, **{
                                        key: row[key] for key in ('pixels_compared', 'pixel_mismatches', 'sha256')}))
            print(json.dumps(row), flush=True)
    report = dict(manifest=manifest, total_pixels_compared=sum(row['pixels_compared'] for row in summary),
                  mismatch_count=sum(row['pixel_mismatches'] for row in summary), cases=comparisons)
    (args.results / 'comparison.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    for name, rows in (('comparison.csv', summary), ('all-metrics.csv', all_metrics)):
        with (args.results / name).open('w', newline='', encoding='utf-8') as stream:
            writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
            writer.writeheader()
            writer.writerows(rows)


if __name__ == '__main__':
    main()
