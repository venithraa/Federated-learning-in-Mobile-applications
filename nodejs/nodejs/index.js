const express = require("express");
const mongoose = require("mongoose");
const multer = require("multer");
const fs = require("fs");
const app = express();
const port = 3000;
const path = require('path');


// Add the fs code here
const checkDirectories = () => {
  // Check if the 'uploads' directory exists
  if (!fs.existsSync("uploads")) {
    console.log("The 'uploads' directory does not exist.");
  } else {
    console.log("The 'uploads' directory exists.");
  }

  // Check if the 'uploads/tflite' directory exists
  if (!fs.existsSync("uploads/tflite")) {
    console.log("The 'uploads/tflite' directory does not exist.");
  } else {
    console.log("The 'uploads/tflite' directory exists.");
  }
};

const userRoute = require("./routes/user");
const commentsRoute = require("./routes/comments");
app.use(express.json());

// Create the multer middleware for general file uploads (e.g., JSON)
const generalUpload = multer({ dest: "uploads/" });

// Create the multer middleware specifically for TFLite file uploads
const tfliteUpload = multer({ dest: "uploads/tflite/" });

app.use("/user", userRoute);
app.use("/comments", commentsRoute);


// Connect to MongoDB
const uri =
  "mongodb+srv://venithraaganesan:Eie11238@datacluster.da1anwh.mongodb.net/locationdb?retryWrites=true&w=majority";

async function connect() {
  try {
    await mongoose.connect(uri);
    console.log("Connected to MongoDB");
  } catch (error) {
    console.error(error);
  }
}

connect();

// Define the location schema
const locationSchema = new mongoose.Schema({
  id: Number,
  isForeground: Boolean,
  latitude: Number,
  longitude: Number,
  altitude: Number,
  timestamp: String,
  signalStrength: String,
});

const Location = mongoose.model("Location", locationSchema);

// Route to handle general file uploads (e.g., JSON)
app.post("/location", generalUpload.fields([{ name: 'jsonFile' }]), async (req, res) => {
  try {
    if (!req.files || !req.files.jsonFile) {
      // No JSON file was uploaded
      return res.status(400).send("JSON file is required");
    }

    // Read and parse the uploaded JSON file
    const fileData = fs.readFileSync(req.files.jsonFile[0].path, "utf8");
    const jsonData = JSON.parse(fileData);

    // Process the data and save it to the database
    if (Array.isArray(jsonData)) {
      // If the data is an array, process each element
      for (const element of jsonData) {
        const location = new Location({
          id: element.id,
          isForeground: element.isForeground,
          latitude: element.latitude,
          longitude: element.longitude,
          altitude: element.altitude,
          timestamp: element.timestamp,
          signalStrength: element.gsmSignalStrength,
        });

        await location.save();
        console.log("Received data as array:", location);
      }
    } else if (typeof jsonData === "object") {
      // If the data is a single object, process it as a single element
      const location = new Location({
        id: jsonData.id,
        isForeground: jsonData.isForeground,
        latitude: jsonData.latitude,
        longitude: jsonData.longitude,
        altitude: jsonData.altitude,
        timestamp: jsonData.timestamp,
        signalStrength: jsonData.gsmSignalStrength,
      });

      await location.save();
      console.log("Received data as object:", location);
    } else {
      throw new Error("Invalid data format");
    }

    // Remove the uploaded JSON file
    fs.unlinkSync(req.files.jsonFile[0].path);

    res.send("Posted JSON data successfully");
  } catch (error) {
    console.error(error);
    res.status(500).send("Error occurred while processing the JSON request");
  }
});

// Route to handle TFLite file uploads
app.post("/tflite", tfliteUpload.single("tfliteFile"), (req, res) => {
  try {
    if (!req.file) {
      // No TFLite file was uploaded
      return res.status(400).send("TFLite file is required");
    }

    // Process the TFLite file (you can save it or perform other operations)
    const tfliteFile = req.file;
    // Example: Save the TFLite file with a unique name
    const tfliteFilePath = `uploads/tflite/${Date.now()}_${tfliteFile.originalname}`;
    fs.renameSync(tfliteFile.path, tfliteFilePath);

    // Optionally, you can perform additional processing or validation here

    res.send("TFLite file uploaded successfully");
  } catch (error) {
    console.error(error);
    res.status(500).send("Error occurred while processing the TFLite request");
  }
});


// Route to retrieve a list of uploaded TFLite files
app.get("/tflite", (req, res) => {
  try {
    const tfliteFiles = fs.readdirSync("uploads/tflite");
    res.json(tfliteFiles);
  } catch (error) {
    console.error(error);
    res.status(500).send("Error occurred while fetching TFLite files");
  }
});


app.listen(port, () => {
  console.log("Server is running fine");
  checkDirectories(); // Call the function to check directories on startup
});
