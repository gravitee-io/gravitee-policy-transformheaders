## Overview
You can use the `transform-headers` policy to override headers in incoming or outgoing traffic.

### Execution order
Header transformations are executed in the following order:

1. Set/replace headers
2. Append headers: add values to existing headers or add a new header
   * This is not supported for Native APIs
3. Remove headers
4. Keep only whitelisted headers

### Header removal
* Headers added/appended by this policy can be removed
* Whitelisting applies to headers added/appended by this policy



## Usage
Here are some usage examples of using Transform Headers. 

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


