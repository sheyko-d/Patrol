<?php

// Retrieve POST parameters
if (isset($_POST['token'])){
	$token = $_POST['token'];
}

// Check if any parameters are empty
if (empty($token)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect("localhost", "root", "", "database_guard");
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);

// Find id of the user with the specified token
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
} else {
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

// Find user in the database with the specified token
$user_query = $db->makeQuery($con, "SELECT assigned_object_id FROM user WHERE user_id='$id'");
$user_result = $user_query->fetch_assoc();
if ($user_result != null){
	$assigned_object_id = $user_result["assigned_object_id"];
} else {
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

$contacts = array();

// Find contacts with the same assigned object id
$contacts_query = $db->makeQuery($con, "SELECT user_id, name, email, photo FROM user WHERE user_id<>'$id' AND assigned_object_id='$assigned_object_id'");
while ($contacts_result = $contacts_query->fetch_assoc()){
	$user_id = $contacts_result["user_id"];
	$name = $contacts_result["name"];
	$email = $contacts_result["email"];
	$photo = $contacts_result["photo"];
	
	array_push($contacts, array("user_id" => $user_id, "name" => $name, "email" => $email, "photo" => $photo));
}

echo json_encode($contacts);

?>