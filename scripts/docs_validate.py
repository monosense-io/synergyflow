#!/usr/bin/env python3
import sys, pathlib, re

def main():
    base = pathlib.Path('docs/prd')
    ok = True
    for p in sorted(base.glob('[0-9][0-9]-*.md')):
        text = p.read_text(encoding='utf-8')
        if not text.lstrip().startswith('---'):
            print(f"Missing front matter: {p}")
            ok = False
        if '## Review Checklist' not in text:
            print(f"Missing Review Checklist: {p}")
            ok = False
        if '## Traceability' not in text:
            print(f"Missing Traceability: {p}")
            ok = False
        m = re.search(r'^##\s+', text, re.M)
        if not m:
            print(f"Missing H2 title: {p}")
            ok = False
    print('Validation:', 'PASS' if ok else 'FAIL')
    sys.exit(0 if ok else 1)

if __name__ == '__main__':
    main()

