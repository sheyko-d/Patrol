<?

// Retrieve POST parameters
if (isset($_POST['token'])){
	$token = $_POST['token'];
}
if (isset($_POST['min'])){
	$min = $_POST['min'];
}

// Check if any parameters are empty
if (empty($token) or empty($min)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);
$min = mysqli_real_escape_string($con, $min);

// Check if user's token is valid to prevent unauthorized access
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
} else { 
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

// Find user in the database with the specified id
$user_query = $db->makeQuery($con, "SELECT name, assigned_object_id FROM user WHERE user_id='$id'");
$user_result = $user_query->fetch_assoc();
$name = $user_result["name"];
$assigned_object_id = $user_result["assigned_object_id"];

$assigned_object_query = $db->makeQuery($con, "SELECT title, admin_emails FROM assigned_object WHERE assigned_object_id='$assigned_object_id'");
if ($assigned_object_result = $assigned_object_query->fetch_assoc()){
	$assigned_object_title = " (".$assigned_object_result["title"].")";
	$admin_emails = $assigned_object_result["admin_emails"];
}

$recipients = $admin_emails;

if ($recipients!=null){
	mail($recipients, 'Removed watch alert', $name.' removed his watch for more than '.$min.' min.');
}

?>