<?php

// Retrieve POST parameters
if (isset($_POST['shift_id'])){
	$shift_id = $_POST['shift_id'];
}

// Check if any parameters are empty
if (empty($shift_id)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$shift_id = mysqli_real_escape_string($con, $shift_id);

// Unassign all users from this object
$db->makeQuery($con, "DELETE FROM assigned_shift WHERE shift_id='$shift_id'") or die(mysqli_error());

// Delete the assigned object with the specified id
$db->makeQuery($con, "DELETE FROM shift WHERE shift_id='$shift_id'") or die(mysqli_error());

?>