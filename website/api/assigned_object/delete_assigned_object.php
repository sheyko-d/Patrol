<?php

// Retrieve POST parameters
if (isset($_POST['assigned_object_id'])){
	$assigned_object_id = $_POST['assigned_object_id'];
}

// Check if any parameters are empty
if (empty($assigned_object_id)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$assigned_object_id = mysqli_real_escape_string($con, $assigned_object_id);

// Unassign all users from this object
$db->makeQuery($con, "UPDATE user SET assigned_object_id = NULL WHERE assigned_object_id='$assigned_object_id'") or die(mysqli_error());

// Delete the assigned object with the specified id
$db->makeQuery($con, "DELETE FROM assigned_object WHERE assigned_object_id='$assigned_object_id'") or die(mysqli_error());

?>