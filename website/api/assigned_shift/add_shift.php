<?php

// Retrieve POST parameters
if (isset($_POST['name'])){
	$name = $_POST['name'];
}
if (isset($_POST['start_time'])){
	$start_time = $_POST['start_time'];
}
if (isset($_POST['end_time'])){
	$end_time = $_POST['end_time'];
}

// Check if any parameters are empty
if (empty($name) or empty($start_time) or empty($end_time)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$name = mysqli_real_escape_string($con, $name);
$start_time = mysqli_real_escape_string($con, $start_time);
$end_time = mysqli_real_escape_string($con, $end_time);

// Unassign user from the object to guard
$db->makeQuery($con, "INSERT INTO shift(name, start_time, end_time) VALUES('$name', '$start_time', '$end_time')") or die(mysqli_error());

?>