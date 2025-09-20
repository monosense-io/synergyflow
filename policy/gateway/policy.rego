package gateway

default deny = []

# Ensure JWT providers declare audiences
deny[msg] {
  input.kind == "JWT"
  count(input.spec.providers) > 0
  p := input.spec.providers[0]
  not p.audiences
  msg := sprintf("JWT provider '%s' must declare at least one audience", [p.name])
}

# Restrict claim-to-header mapping to approved headers only
deny[msg] {
  input.kind == "JWT"
  some i
  hdr := input.spec.claimToHeaders[i]
  allowed := {"x-auth-sub", "x-auth-roles"}
  not allowed[lower(hdr.header)]
  msg := sprintf("JWT claimToHeaders contains disallowed header '%s'", [hdr.header])
}

# Require JWT filter on /api routes in HTTPRoute
deny[msg] {
  input.kind == "HTTPRoute"
  some r
  rinfo := input.spec.rules[r]
  some m
  pm := rinfo.matches[m]
  pm.path.type == "PathPrefix"
  startswith(pm.path.value, "/api")
  # No JWT filter present in filters
  not jwt_filter_present(rinfo.filters)
  msg := "HTTPRoute rules for '/api' must include JWT filter"
}

jwt_filter_present(filters) {
  some i
  f := filters[i]
  f.type == "ExtensionRef"
  f.extensionRef.kind == "JWT"
}

