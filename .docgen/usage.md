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

### Remove headers

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