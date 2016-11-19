<?php

// Retrieve POST parameters
if (isset($_POST['name'])){
	$name = $_POST['name'];
}
if (isset($_POST['email'])){
	$email = $_POST['email'];
}
if (isset($_POST['password'])){
	$password = $_POST['password'];
}
if (isset($_POST['photo'])){
	$photo = $_POST['photo'];
} else {
	$photo = null;
}

// Check if any parameters are empty
if (empty($name) or empty($email) or empty($password) or empty($password)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$name = mysqli_real_escape_string($con, $name);
$email = mysqli_real_escape_string($con, $email);
$password = mysqli_real_escape_string($con, $password);
$photo = mysqli_real_escape_string($con, $photo);

// Generate a password salt
$password_salt = generatePasswordSalt();

// Encode the password
$password = sha1($password.$password_salt);

// Find user in the database with the specified email and password
$user_query = $db->makeQuery($con, "SELECT user_id FROM user WHERE email='$email'");

// Check if user exists
$user_result = $user_query->fetch_assoc();
if ($user_result == null){
	if (!mysqli_query($con, "INSERT INTO user (name, email, password, password_salt) VALUES ('$name', '$email',
							'$password', '$password_salt')")){
		http_response_code(409);
		die(json_encode(array("message"=>"Can't create new user.")));
	}
	$id = mysqli_insert_id($con);
	$token = updateToken($db, $con, $id);
	
	http_response_code(200);
	echo json_encode(array("token"=>$token, "user_id"=>$id, "name"=>$name, "email"=>$email, "photo"=>$photo));
} else {
	http_response_code(403);
	die(json_encode(array("message"=>"Email is already taken.")));
}

// Generates a random string which will be attached to password before encoding to keep it secure
function generatePasswordSalt() {
    $alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890';
    $pass = array(); //remember to declare $pass as an array
    $alphaLength = strlen($alphabet) - 1; //put the length -1 in cache
    for ($i = 0; $i < 8; $i++) {
        $n = rand(0, $alphaLength);
        $pass[] = $alphabet[$n];
    }
    return implode($pass); //turn the array into a string
}

function updateToken($db, $con, $id){
	$secret = generateNewToken();
	
	$token_query = $db->makeQuery($con, "SELECT secret FROM token WHERE user_id='$id'");
	$token_result = $token_query->fetch_assoc();
	if ($token_result != null){
		$token_query = $db->makeQuery($con, "UPDATE token SET secret='$secret' WHERE user_id='$id'");
	} else {
		$db->makeQuery($con, "INSERT INTO token(user_id, secret) values('$id', '$secret')");
	}
	
	return $secret;
}

// Generates a random string which will be attached to password before encoding to keep it secure
function generateNewToken() {
    $alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890';
    $pass = array(); //remember to declare $pass as an array
    $alphaLength = strlen($alphabet) - 1; //put the length -1 in cache
    for ($i = 0; $i < 30; $i++) {
        $n = rand(0, $alphaLength);
        $pass[] = $alphabet[$n];
    }
    return implode($pass); //turn the array into a string
}

?>