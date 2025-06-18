Here are some usage examples of using {{ .Plugin.Title }}. 

Although each transformation can be configured individually, examples below emphasise that they can be cumulative.

### Set/replace headers

Given the following headers:
```
Content-Type: application/json
```

When applying 'set/replace' with:
* `X-Hello` and value `World`
* `Content-Type` and value `*/*`

Then headers are transformed as follows:
```
X-Hello: World
Content-Type: */*
```

### Amend headers

Given the following headers:
```
X-Hello: World
Content-Type: */*
```

When applying 'amend' with:
* `X-Hello` and value `Good morning`
* `X-Extra` and value `Superfluous` 

Then headers are transformed as follows:
```
X-Hello: World,Good morning
X-Extra: Superfluous
Content-Type: */*
```

### Header removal

Given the following headers:
```
X-Hello: World,Good morning
X-Extra: Superfluous
Content-Type: */*
```

When applying 'remove' with:
* name `X-Extra`

Then headers are transformed as follows:
```
X-Hello: World,Good morning
Content-Type: */*
```

### Keep only whitelisted headers

Given the following headers:
```
X-Hello: World,Good morning
Content-Type: */*
```

When applying 'whitelisting' with:

* name `Content-Type`

Then headers are transformed as follows:
```
Content-Type: */*
```

### Native Kafka API Usage

For Native Kafka APIs, the transform-headers policy works with Kafka record headers instead of HTTP headers. Here are examples for both publish and subscribe phases:

#### Publish Phase Example

Given the following Kafka record headers:
```
X-Correlation-Id: abc-123
X-Internal-Header: debug-info
```

When applying 'set/replace' with:
* `X-Gravitee-Request-Id` and value `{#request.id}`
* `X-Source-System` and value `api-gateway`

And removing:
* `X-Internal-Header`

Then headers are transformed as follows:
```
X-Correlation-Id: abc-123
X-Gravitee-Request-Id: req-456
X-Source-System: api-gateway
```

#### Subscribe Phase Example

Given the following Kafka record headers:
```
X-Correlation-Id: abc-123
X-Debug-Header: debug-info
Content-Type: application/json
```

When applying 'set/replace' with:
* `X-Processing-Timestamp` and value `{#date.now()}`

And removing:
* `X-Debug-Header`

Then headers are transformed as follows:
```
X-Correlation-Id: abc-123
X-Processing-Timestamp: 2024-01-15T10:30:00Z
Content-Type: application/json
```

**Note:** Append headers functionality is not supported for Native Kafka APIs.