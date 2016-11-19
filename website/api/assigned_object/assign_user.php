<?php

// Retrieve POST parameters
if (isset($_POST['id'])){
	$id = $_POST['id'];
}
if (isset($_POST['assigned_object_id'])){
	$assigned_object_id = $_POST['assigned_object_id'];
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
if (isset($_POST['contacts'])){
	$contacts = $_POST['contacts'];
}
if (isset($_POST['safety'])){
	$safety = $_POST['safety'];
}
if (isset($_POST['video'])){
	$video = $_POST['video'];
}
if (isset($_POST['sitting_duration'])){
	$sitting_duration = $_POST['sitting_duration'];
}
if (isset($_POST['watch_removed_max_min'])){
	$watch_removed_max_min = $_POST['watch_removed_max_min'];
}
if (isset($_POST['leave_watch_message'])){
	$leave_watch_message = $_POST['leave_watch_message'];
}
if (isset($_POST['admin_emails'])){
	$admin_emails = $_POST['admin_emails'];
}

// Check if any parameters are empty
if (empty($latitude) or empty($longitude) or empty($title)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$id = mysqli_real_escape_string($con, $id);
$assigned_object_id = mysqli_real_escape_string($con, $assigned_object_id);
$title = mysqli_real_escape_string($con, $title);
$latitude = mysqli_real_escape_string($con, $latitude);
$longitude = mysqli_real_escape_string($con, $longitude);
$contacts = mysqli_real_escape_string($con, $contacts);
$safety = mysqli_real_escape_string($con, $safety);
$video = mysqli_real_escape_string($con, $video);
$sitting_duration = mysqli_real_escape_string($con, $sitting_duration);
$watch_removed_max_min = mysqli_real_escape_string($con, $watch_removed_max_min);
$leave_watch_message = mysqli_real_escape_string($con, $leave_watch_message);
$admin_emails = mysqli_real_escape_string($con, $admin_emails);

if ($assigned_object_id=="" or $assigned_object_id==null){
	// Find places with the same exact location
	$assigned_object_query = $db->makeQuery($con, "SELECT assigned_object_id, title, latitude, longitude FROM assigned_object WHERE latitude='$latitude' AND longitude='$longitude'");

	// Check if assigned object exists
	$assigned_object_result = $assigned_object_query->fetch_assoc();

	if ($assigned_object_result == null){
		// Find places within 100 m. to the selected user location
		$assigned_object_query = $db->makeQuery($con, "SELECT
		  assigned_object_id, title, latitude, longitude, contacts, safety, video, sitting_duration, watch_removed_max_min, leave_watch_message, admin_emails, (
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
		$safety = $assigned_object_result["safety"];
		$video = $assigned_object_result["video"];
		$sitting_duration = $assigned_object_result["sitting_duration"];
		$watch_removed_max_min = $assigned_object_result["watch_removed_max_min"];
		$leave_watch_message = $assigned_object_result["leave_watch_message"];
		$admin_emails = $assigned_object_result["admin_emails"];
	} else {
		if (!mysqli_query($con, "INSERT INTO assigned_object (title, latitude, longitude, contacts, safety, video, sitting_duration, watch_removed_max_min, leave_watch_message, admin_emails) VALUES ('$title',
								'$latitude', '$longitude', '$contacts', '$safety', '$video', '$sitting_duration', '$watch_removed_max_min', '$leave_watch_message', '$admin_emails')")){
			http_response_code(409);
			die(json_encode(array("message"=>"Can't create a new place: ")));
		}
		
		$assigned_object_id = mysqli_insert_id($con);
	}
	$assigned_object = array("assigned_object_id"=>$assigned_object_id, "title"=>$title, "latitude"=>$latitude, "longitude"=>$longitude, "contacts"=>$contacts, "safety"=>$safety, "video"=>$video, "sitting_duration"=>$sitting_duration,
							 "watch_removed_max_min"=>$watch_removed_max_min, "leave_watch_message"=>$leave_watch_message, "admin_emails"=>$admin_emails);

	mysqli_query($con, "UPDATE user SET assigned_object_id='$assigned_object_id' WHERE user_id='$id'") or die(mysqli_error($con));
	echo json_encode($assigned_object);
} else {
	mysqli_query($con, "UPDATE assigned_object SET title='$title', contacts='$contacts', safety='$safety', video='$video', sitting_duration='$sitting_duration', watch_removed_max_min='$watch_removed_max_min', leave_watch_message='$leave_watch_message', admin_emails='$admin_emails' WHERE assigned_object_id='$assigned_object_id'") or die(mysqli_error($con));
}

?>