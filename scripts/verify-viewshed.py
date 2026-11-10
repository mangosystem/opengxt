"""Independent DEM reference: intersect each candidate cell rectangle with the sightline.

Requires numpy and rasterio. It does not use the Java DDA traversal or its visibility code.
Checks every output cell at 1 km, and deterministic random/aligned samples at other ranges.
"""
import argparse
import csv
import hashlib
import json
from pathlib import Path

import numpy as np
import rasterio


def reference(z, ox, oy, tx, ty, offset):
    sx, sy = int(np.floor(ox)), int(np.floor(oy))
    if tx == sx and ty == sy:
        return 1
    if not np.isfinite(z[ty, tx]):
        return 255
    x0, x1 = sorted((sx, tx))
    y0, y1 = sorted((sy, ty))
    xx = np.arange(x0, x1 + 1, dtype=float)[None, :]
    yy = np.arange(y0, y1 + 1, dtype=float)[:, None]
    dx, dy = tx + .5 - ox, ty + .5 - oy
    if dx == 0:
        xenter, xexit = np.zeros_like(xx), np.ones_like(xx)
    else:
        a, b = (xx - ox) / dx, (xx + 1 - ox) / dx
        xenter, xexit = np.minimum(a, b), np.maximum(a, b)
    if dy == 0:
        yenter, yexit = np.zeros_like(yy), np.ones_like(yy)
    else:
        a, b = (yy - oy) / dy, (yy + 1 - oy) / dy
        yenter, yexit = np.minimum(a, b), np.maximum(a, b)
    enter = np.maximum(0, np.maximum(xenter, yenter))
    leave = np.minimum(1, np.minimum(xexit, yexit))
    crossed = leave - enter > 1e-12
    crossed[sy - y0, sx - x0] = False
    crossed[ty - y0, tx - x0] = False
    ground = z[y0:y1 + 1, x0:x1 + 1][crossed]
    start_height = z[sy, sx] + offset
    gradient = z[ty, tx] - start_height
    sight_min = np.minimum(start_height + gradient * enter[crossed],
                           start_height + gradient * leave[crossed])
    if np.any(ground - sight_min > 1e-9):
        return 0
    return 255 if np.any(~np.isfinite(ground)) else 1


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('dem', type=Path)
    parser.add_argument('results', type=Path)
    parser.add_argument('--samples', type=int, default=1024)
    args = parser.parse_args()
    reports = []
    with rasterio.open(args.dem) as source:
        z = source.read(1).astype(float)
        z[~source.read_masks(1).astype(bool)] = np.nan
        sy, sx = source.height // 2, source.width // 2
        observer = source.xy(sy, sx)
        ox, oy = (~source.transform) * observer
        all_y, all_x = np.indices(z.shape)
        world_x = source.transform.a * (all_x + .5) + source.transform.b * (all_y + .5) + source.transform.c
        world_y = source.transform.d * (all_x + .5) + source.transform.e * (all_y + .5) + source.transform.f
        distances = np.hypot(world_x - observer[0], world_y - observer[1])
        for radius in (1000, 5000, 10000, 0):
            folder = args.results / str(radius)
            with rasterio.open(folder / 'viewshed.tif') as result:
                actual = result.read(1)
                assert result.crs == source.crs, 'CRS changed'
                assert result.nodata == 255, 'NoData metadata missing'
                assert result.dtypes == ('uint8',), result.dtypes
                assert result.transform.a == source.transform.a
                assert result.transform.e == source.transform.e
                xoff, yoff = (~source.transform) * (result.transform.c, result.transform.f)
                x0, y0 = int(round(xoff)), int(round(yoff))
                assert abs(xoff - x0) < 1e-7 and abs(yoff - y0) < 1e-7
                included = distances <= radius if radius else np.ones(z.shape, dtype=bool)
                included[sy, sx] = True
                ys, xs = np.where(included)
                assert (x0, y0, actual.shape[1], actual.shape[0]) == (
                    xs.min(), ys.min(), xs.max() - xs.min() + 1, ys.max() - ys.min() + 1)
                if radius == 1000:
                    selected = set(range(actual.size))
                else:
                    rng = np.random.default_rng(17 + radius)
                    selected = set(map(int, rng.choice(actual.size, min(args.samples, actual.size), replace=False)))
                    # Entire observer row/column plus diagonal/corner crossings.
                    selected.update((sy - y0) * actual.shape[1] + x for x in range(actual.shape[1]))
                    selected.update(y * actual.shape[1] + sx - x0 for y in range(actual.shape[0]))
                    for length in (1, 2, 3, 10, 33, 50, 100, 200, 300, 400):
                        for dx in (-1, 1):
                            for dy in (-1, 1):
                                x, y = sx + dx * length - x0, sy + dy * length - y0
                                if 0 <= x < actual.shape[1] and 0 <= y < actual.shape[0]:
                                    selected.add(y * actual.shape[1] + x)
                mismatches = []
                for index in sorted(selected):
                    y, x = divmod(index, actual.shape[1])
                    tx, ty = x0 + x, y0 + y
                    expected = reference(z, ox, oy, tx, ty, 1.6) if included[ty, tx] else 255
                    if int(actual[y, x]) != expected:
                        mismatches.append(dict(x=tx, y=ty, expected=expected, actual=int(actual[y, x])))
                # All-cell invariants, not just sampled accuracy checks.
                local_z = z[y0:y0 + actual.shape[0], x0:x0 + actual.shape[1]]
                local_included = included[y0:y0 + actual.shape[0], x0:x0 + actual.shape[1]]
                assert np.all(actual[~local_included | ~np.isfinite(local_z)] == 255)
                assert actual[sy - y0, sx - x0] == 1
                assert set(map(int, np.unique(actual))) <= {0, 1, 255}
                checksum = hashlib.sha256(actual.tobytes()).hexdigest()
                with (folder / 'metrics.csv').open() as stream:
                    metrics = list(csv.DictReader(stream))
                assert len(metrics) == 4
                assert all(row['sha256'] == checksum for row in metrics), 'Runs were not deterministic'
                report = dict(radius_m=radius, shape=list(actual.shape), checked_cells=len(selected),
                              mismatch_count=len(mismatches), mismatches=mismatches[:20],
                              sha256=checksum, visible=int(np.count_nonzero(actual == 1)),
                              invisible=int(np.count_nonzero(actual == 0)), nodata=int(np.count_nonzero(actual == 255)))
                reports.append(report)
                print(json.dumps(report), flush=True)
    output = dict(dem=str(args.dem), dem_sha256=hashlib.sha256(args.dem.read_bytes()).hexdigest(),
                  method='Independent ray/rectangle slab intersections; piecewise constant DEM; 1e-9 m tolerance',
                  seed=17, cases=reports)
    (args.results / 'accuracy.json').write_text(json.dumps(output, indent=2), encoding='utf-8')
    assert not any(row['mismatch_count'] for row in reports), 'Visibility mismatches found'


if __name__ == '__main__':
    main()
