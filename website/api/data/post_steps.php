<?php

// Retrieve POST parameters
if (isset($_POST['step_values'])){
	$step_values = json_decode($_POST['step_values'], true);
} else {
	$step_values = null;
}
if (isset($_POST['token'])){
	$token = $_POST['token'];
}

// Check if any parameters are empty
if (empty($token) or empty($step_values)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);

// Check if user's token is valid to prevent unauthorized access
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
} else { 
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

for ($i=0; $i<count($step_values); $i++){
	$time = $step_values[$i]["time"]/1000;
	$value = $step_values[$i]["value"];
	mysqli_query($con, "INSERT INTO step(user_id, time, value) VALUES ('$id', '$time', '$value')");
}

?>