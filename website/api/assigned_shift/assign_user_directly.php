<?php

// Retrieve POST parameters
if (isset($_POST['user_id'])){
	$user_id = $_POST['user_id'];
}
if (isset($_POST['shift_id'])){
	$shift_id = $_POST['shift_id'];
}

// Check if any parameters are empty
if (empty($user_id) or empty($shift_id)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$user_id = mysqli_real_escape_string($con, $user_id);
$shift_id = mysqli_real_escape_string($con, $shift_id);

// Unassign user from the object to guard
$db->makeQuery($con, "INSERT INTO assigned_shift(user_id, shift_id) VALUES('$user_id', '$shift_id')") or die(mysqli_error());

?>