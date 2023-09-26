const express = require("express")
const router = express.Router()
var fs = require('fs')

router.get('/', function(req,res) {
    fs.readFile(__dirname + "/" + "comments.json", 'utf8', function(err, data)
    {
      console.log(data)
      res.end(data) })
  });


module.exports = router;