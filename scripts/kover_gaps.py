#!/usr/bin/env python3
"""List uncovered lines/branches per source file from a Kover (JaCoCo XML) report.

usage: kover_gaps.py <report.xml> [--all]   (--all also prints fully covered files)
"""
import sys
import xml.etree.ElementTree as ET


def main() -> None:
    path = sys.argv[1]
    show_all = "--all" in sys.argv
    root = ET.parse(path).getroot()
    total_ml = total_cl = total_mb = total_cb = 0
    rows = []
    for pkg in root.iter("package"):
        for sf in pkg.findall("sourcefile"):
            name = f"{pkg.get('name')}/{sf.get('name')}"
            missed_lines, partial = [], []
            ml = cl = mb = cb = 0
            for line in sf.findall("line"):
                nr = int(line.get("nr"))
                mi, ci = int(line.get("mi")), int(line.get("ci"))
                mbr, cbr = int(line.get("mb")), int(line.get("cb"))
                if ci == 0 and mi > 0:
                    ml += 1
                    missed_lines.append(nr)
                else:
                    cl += 1
                mb += mbr
                cb += cbr
                if mbr > 0 and ci > 0:
                    partial.append(f"{nr}({mbr}/{mbr + cbr})")
            total_ml += ml
            total_cl += cl
            total_mb += mb
            total_cb += cb
            if ml or mb or show_all:
                rows.append((name, ml, cl, mb, cb, missed_lines, partial))
    for name, ml, cl, mb, cb, missed, partial in sorted(rows, key=lambda r: (-(r[1] + r[3]), r[0])):
        print(f"{name}: lines {cl}/{cl + ml} branches {cb}/{cb + mb}")
        if missed:
            print(f"    missed lines: {compress(missed)}")
        if partial:
            print(f"    partial branches: {' '.join(partial)}")
    print(f"TOTAL lines {total_cl}/{total_cl + total_ml}  branches {total_cb}/{total_cb + total_mb}")


def compress(nums: list[int]) -> str:
    out, start, prev = [], nums[0], nums[0]
    for n in nums[1:] + [None]:
        if n is not None and n == prev + 1:
            prev = n
            continue
        out.append(str(start) if start == prev else f"{start}-{prev}")
        if n is not None:
            start = prev = n
    return ",".join(out)


if __name__ == "__main__":
    main()
