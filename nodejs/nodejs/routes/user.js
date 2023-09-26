const express = require("express")
const router = express.Router()
var fs = require('fs')

router.get('/', function(req,res) {
  fs.readFile(__dirname + "/" + "user.json", 'utf8', function(err, data)
  {
    console.log(data)
    res.end(data) })
});
router.post('/', function(req,res) {
    console.log(req.body)
    res.send(req.body) 
});

module.exports = router;