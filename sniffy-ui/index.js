var express = require('express');
var cors = require('cors');
var app = express();

app.use(cors({exposedHeaders: ['Sniffy-Sql-Queries', 'Sniffy-Request-Details', 'Sniffy-Time-To-First-Byte']}));
//app.use(cors());

var counter = 1000;
var persistent = false;

app.get('/mock/ajax.json', function (req, res) {
  res.header('Sniffy-Sql-Queries' , 2 );
  res.header('Sniffy-Time-To-First-Byte' , 21 );
  //res.header('Sniffy-Request-Id' , 'a54b32e7-b94b-450b-b145-0cf62270d32a' );
  res.header('Sniffy-Request-Details' , 'request/a54b32e7-b94b-450b-b145-0cf62270d32a' );
  res.send('{"foo":"bar","baz":42}');
});

app.get('/mock/path/subpath/ajax.json', function (req, res) {
  res.header('Sniffy-Sql-Queries' , 2 );
  res.header('Sniffy-Time-To-First-Byte' , 21 );
  //res.header('Sniffy-Request-Id' , 'a54b32e7-b94b-450b-b145-0cf62270d32a' );
  res.header('Sniffy-Request-Details' , '../../request/a54b32e7-b94b-450b-b145-0cf62270d32a' );
  res.send('{"foo":"bar","baz":42}');
});

app.get('/mock/notrailingslash', function (req, res) {
  res.header('Sniffy-Sql-Queries' , 2 );
  res.header('Sniffy-Time-To-First-Byte' , 21 );
  //res.header('Sniffy-Request-Id' , 'a54b32e7-b94b-450b-b145-0cf62270d32a' );
  res.header('Sniffy-Request-Details' , './request/a54b32e7-b94b-450b-b145-0cf62270d32a' );
  res.send('{"foo":"bar","baz":42}');
});

app.get('/mock/204.json', function (req, res) {
  res.header('Sniffy-Sql-Queries' , 1 );
  res.header('Sniffy-Time-To-First-Byte' , 21 );
  //res.header('Sniffy-Request-Id' , 'a54b32e7-b94b-450b-b145-0cf62270d32a' );
  res.header('Sniffy-Request-Details' , 'request/b43a32e7-b94b-450b-b145-0cf62270d32a' );
  res.send('{"foo":"bar","baz":42}');
});

app.get('/mock/connectionregistry/', function (req, res) {
  res.status(200);
  res.header('Content-Type','application/json');
  res.send('{"persistent":' + persistent + ',"sockets":[{"host":"en.wikipedia.org","port":"443","status":100},{"host":"192.168.99.100","port":"3306","status":-50}]' 
    + ',"dataSources":[{"url":"jdbc:h2:mem:/something:","userName":"sa","status":0}]'
    + '}');
});

app.get('/mock/topsql/', function (req, res) {
  res.status(200);
  res.header('Content-Type','application/json');
  res.send('[{"sql":"select visits0_.pet_id as pet_id4_6_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_, visits0_.pet_id as pet_id4_6_1_ from visits visits0_ where visits0_.pet_id=?","timer":{"count":104,"min":0,"median":0,"mean":274962.409564464,"max":1000000,"p75":1000000,"p95":1000000,"p99":1000000}},{"sql":"select distinct owner0_.id as id1_0_0_, pets1_.id as id1_1_1_, owner0_.first_name as first_na2_0_0_, owner0_.last_name as last_nam3_0_0_, owner0_.address as address4_0_0_, owner0_.city as city5_0_0_, owner0_.telephone as telephon6_0_0_, pets1_.name as name2_1_1_, pets1_.birth_date as birth_da3_1_1_, pets1_.owner_id as owner_id4_1_1_, pets1_.type_id as type_id5_1_1_, pets1_.owner_id as owner_id4_1_0__, pets1_.id as id1_1_0__ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.last_name like ?","timer":{"count":8,"min":1000000,"median":1000000,"mean":1481153.596288246,"max":3000000,"p75":2000000,"p95":2000000,"p99":3000000}},{"sql":"select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=?","timer":{"count":48,"min":0,"median":0,"mean":396214.9203078056,"max":1000000,"p75":1000000,"p95":1000000,"p99":1000000}}]');
});

app.post('/mock/connectionregistry/socket/*', function (req, res) {
  res.status(201);
  res.send();
});
app.post('/mock/connectionregistry/datasource/*', function (req, res) {
  res.status(201);
  res.send();
});
app.post('/mock/connectionregistry/persistent/', function (req, res) {
  persistent = true;
  res.status(201);
  res.send();
});

app.delete('/mock/connectionregistry/socket/*', function (req, res) {
  res.status(201);
  res.send();
});
app.delete('/mock/connectionregistry/datasource/*', function (req, res) {
  res.status(201);
  res.send();
});
app.delete('/mock/connectionregistry/persistent/', function (req, res) {
  persistent = false;
  res.status(201);
  res.send();
});
app.delete('/mock/topsql/', function (req, res) {
  res.status(201);
  res.send();
});

app.use('/mock', express.static('mock'));

var server = app.listen(3000, function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('Example app listening at http://%s:%s', host, port);
});

var server2 = app.listen(3001, function () {
  var host = server2.address().address;
  var port = server2.address().port;

  console.log('Example app listening at http://%s:%s', host, port);
});