<?php

// Retrieve POST parameters
if (isset($_POST['user_id'])){
	$user_id = $_POST['user_id'];
}

// Check if any parameters are empty
if (empty($user_id)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$user_id = mysqli_real_escape_string($con, $user_id);

// Unassign user from the object to guard
$db->makeQuery($con, "UPDATE user SET assigned_object_id = NULL WHERE user_id='$user_id'") or die(mysqli_error());

?>