'use strict';

// Add this to the very top of the first file loaded in your app
var apm = require('elastic-apm-node').start({
  serviceName: 'hellonode',
  secretToken: 'zXDQFwaaVEoDeAXGXc',
  serverUrl: 'https://d59f8057afe0404e957ead0bbfa99a30.apm.eu-west-1.aws.cloud.es.io:443',
  environment: 'Demo'
})

const express = require('express')
const app = express();

const port = 9999;
const host = '0.0.0.0';

app.get('/', (req, res) => {
  res.send('Hello World from kofismartgh');
})

app.listen(port, host);
console.log(`Running on http://${host}:${port}`);
