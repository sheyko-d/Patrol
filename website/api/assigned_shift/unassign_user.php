<?php

// Retrieve POST parameters
if (isset($_POST['assigned_shift_id'])){
	$assigned_shift_id = $_POST['assigned_shift_id'];
}

// Check if any parameters are empty
if (empty($assigned_shift_id)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$assigned_shift_id = mysqli_real_escape_string($con, $assigned_shift_id);

// Unassign user from the object to guard
$db->makeQuery($con, "DELETE FROM assigned_shift WHERE assigned_shift_id='$assigned_shift_id'") or die(mysqli_error());

?>