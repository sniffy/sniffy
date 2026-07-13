![Build and deploy](https://github.com/sniffy/sniffy-ui/workflows/Build%20and%20deploy/badge.svg)
[![Dependency Status](https://david-dm.org/sniffy/sniffy-ui.svg)](https://david-dm.org/sniffy/sniffy-ui)
[![devDependency Status](https://david-dm.org/sniffy/sniffy-ui/dev-status.svg)](https://david-dm.org/sniffy/sniffy-ui#info=devDependencies)

# sniffy-ui
Sniffy UI 

## Prerequisites

Node and NPM
Grunt CLI (`npm install -g grunt-cli`)

## Building

```
npm install
./node_modules/.bin/bower install
grunt
```

## Developing

```
grunt watch
```

## Testing with mock server

```
node index.js
```

Now open [http://localhost:3000/mock/mock.html](http://localhost:3000/mock/mock.html) in your browser and have fun!