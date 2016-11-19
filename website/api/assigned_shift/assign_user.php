<?php

// Retrieve POST parameters
if (isset($_POST['id'])){
	$id = $_POST['id'];
}
if (isset($_POST['title'])){
	$title = $_POST['title'];
}
if (isset($_POST['latitude'])){
	$latitude = $_POST['latitude'];
}
if (isset($_POST['longitude'])){
	$longitude = $_POST['longitude'];
}

// Check if any parameters are empty
if (empty($id) or empty($latitude) or empty($longitude) or empty($title)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$id = mysqli_real_escape_string($con, $id);
$title = mysqli_real_escape_string($con, $title);
$latitude = mysqli_real_escape_string($con, $latitude);
$longitude = mysqli_real_escape_string($con, $longitude);

// Find places with the same exact location
$assigned_object_query = $db->makeQuery($con, "SELECT assigned_object_id, title, latitude, longitude FROM assigned_object WHERE latitude='$latitude' AND longitude='$longitude'");

// Check if assigned object exists
$assigned_object_result = $assigned_object_query->fetch_assoc();

if ($assigned_object_result == null){
	// Find places within 100 m. to the selected user location
	$assigned_object_query = $db->makeQuery($con, "SELECT
	  assigned_object_id, title, latitude, longitude, (
		6371 * acos (
		  cos ( radians('$latitude') )
		  * cos( radians( latitude ) )
		  * cos( radians( longitude ) - radians('$longitude') )
		  + sin ( radians('$latitude') )
		  * sin( radians( latitude ) )
		)
	  ) AS distance
	FROM assigned_object
	HAVING distance < 0.1
	ORDER BY distance
	LIMIT 0 , 1;");
}

// Check if assigned object exists
$assigned_object_result = $assigned_object_query->fetch_assoc();
if ($assigned_object_result != null){
	$assigned_object_id = $assigned_object_result["assigned_object_id"];
	$title = $assigned_object_result["title"];
	$latitude = $assigned_object_result["latitude"];
	$longitude = $assigned_object_result["longitude"];
	$assigned_object = array("assigned_object_id"=>$assigned_object_id, "title"=>$title, "latitude"=>$latitude,
								 "longitude"=>$longitude);
} else {
	if (!mysqli_query($con, "INSERT INTO assigned_object (title, latitude, longitude) VALUES ('$title',
							'$latitude', '$longitude')")){
		http_response_code(409);
		die(json_encode(array("message"=>"Can't create a new place.")));
	}
	
	$assigned_object_id = mysqli_insert_id($con);
	$assigned_object = array("assigned_object_id"=>$assigned_object_id, "title"=>$title, "latitude"=>$latitude,
							 "longitude"=>$longitude);
}

if (!mysqli_query($con, "UPDATE user SET assigned_object_id='$assigned_object_id' WHERE user_id='$id'")){
	http_response_code(409);
	die(json_encode(array("message"=>"Can't create a new place.")));
}

http_response_code(200);
echo json_encode($assigned_object);

?>