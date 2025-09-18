#!/usr/bin/env python3
import sys
import os
from pathlib import Path
import copy
import yaml

ROOT = Path(__file__).resolve().parents[1]
API_DIR = ROOT / 'docs' / 'api'
MODULES_DIR = API_DIR / 'modules'
AGG_OUT = API_DIR / 'openapi.yaml'

MODULE_FILES = [
    'incidents.yaml',
    'problems.yaml',
    'changes.yaml',
    'service-requests.yaml',
    'knowledge.yaml',
    'users.yaml',
    'teams.yaml',
    'cmdb.yaml',
    'search.yaml',
    'events.yaml',
    'system.yaml',
]

def deep_merge(dst, src, prefer_dst=True):
    for k, v in src.items():
        if isinstance(v, dict) and isinstance(dst.get(k), dict):
            deep_merge(dst[k], v, prefer_dst)
        else:
            if prefer_dst and k in dst:
                continue
            dst[k] = copy.deepcopy(v)

def ensure_components(agg):
    agg.setdefault('components', {})
    for key in ['schemas','parameters','responses','headers','securitySchemes']:
        agg['components'].setdefault(key, {})

def collect_tags_from_paths(paths):
    tags = set()
    for _, path_item in paths.items():
        if not isinstance(path_item, dict):
            continue
        for method, op in path_item.items():
            if method.lower() not in ['get','post','put','patch','delete','options','head','trace']:
                continue
            for t in op.get('tags', []) or []:
                tags.add(t)
    return [{'name': t} for t in sorted(tags)]

def main():
    # Load shared components
    with open(MODULES_DIR / 'shared.yaml', 'r') as f:
        shared = yaml.safe_load(f)

    aggregate = {
        'openapi': '3.1.0',
        'info': {
            'title': 'SynergyFlow Platform API',
            'version': '1.0.0',
            'summary': 'Aggregated REST + Webhooks API for SynergyFlow ITSM',
            'description': 'Aggregated specification composed from per-module specs under docs/api/modules',
        },
        'servers': [
            {'url': 'https://api.dev.synergyflow.example.com/api/v1', 'description': 'Dev'},
            {'url': 'https://api.synergyflow.example.com/api/v1', 'description': 'Production'},
        ],
        'security': [
            {'bearerAuth': []},
            {'oauth2': []},
            {'ApiKeyAuth': []},
        ],
        'paths': {},
        'components': {},
    }

    # Seed shared components
    ensure_components(aggregate)
    ensure_components(shared)
    for key, value in shared.get('components', {}).items():
        if isinstance(value, dict):
            aggregate['components'].setdefault(key, {})
            deep_merge(aggregate['components'][key], value, prefer_dst=True)

    webhooks = {}

    # Merge modules
    for name in MODULE_FILES:
        path = MODULES_DIR / name
        if not path.exists():
            print(f"Warning: module spec missing: {name}", file=sys.stderr)
            continue
        with open(path, 'r') as f:
            mod = yaml.safe_load(f) or {}
        # Paths
        for p, item in (mod.get('paths') or {}).items():
            if p in aggregate['paths']:
                # Prefer first seen; warn on conflict
                print(f"Warning: duplicate path {p} in {name}", file=sys.stderr)
            else:
                aggregate['paths'][p] = item
        # Components
        ensure_components(aggregate)
        for comp_key, comp_val in (mod.get('components') or {}).items():
            if isinstance(comp_val, dict):
                aggregate['components'].setdefault(comp_key, {})
                deep_merge(aggregate['components'][comp_key], comp_val, prefer_dst=True)
        # Webhooks
        if 'webhooks' in mod:
            for hook, hook_def in (mod.get('webhooks') or {}).items():
                if hook not in webhooks:
                    webhooks[hook] = hook_def

    # Tags from paths
    aggregate['tags'] = collect_tags_from_paths(aggregate['paths'])

    if webhooks:
        aggregate['webhooks'] = webhooks

    # Dump aggregated spec
    AGG_OUT.parent.mkdir(parents=True, exist_ok=True)
    with open(AGG_OUT, 'w') as f:
        yaml.safe_dump(aggregate, f, sort_keys=False)
    print(f"Wrote aggregated OpenAPI: {AGG_OUT}")

if __name__ == '__main__':
    main()

