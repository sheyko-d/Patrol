<?
date_default_timezone_set('GMT-6');
$today_start_ts = strtotime(date('Y-m-d', time()). '00:00:00');
$yesterday_start_ts = $today_start_ts-24*60*60;
$day_1_ago_start_ts = $yesterday_start_ts-24*60*60;
$day_2_ago_start_ts = $day_1_ago_start_ts-24*60*60;
$day_3_ago_start_ts = $day_2_ago_start_ts-24*60*60;
$day_4_ago_start_ts = $day_3_ago_start_ts-24*60*60;
$day_5_ago_start_ts = $day_4_ago_start_ts-24*60*60;
$day_6_ago_start_ts = $day_5_ago_start_ts-24*60*60;

if (isset($_POST["start_date"])){
	$start_date = strtotime($_POST["start_date"]);
}
if (isset($_POST["end_date"])){
	$end_date = strtotime($_POST["end_date"]);
}

if ($start_date == null){
	$start_date = $today_start_ts;
}
if ($end_date == null){
	$end_date = $today_start_ts+24*60*60;
}
?>

<head>
	<title>Stigg Dashboard</title>
	<link rel="stylesheet" type="text/css" href="css/style.css"/>
	<link href='http://fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>
	<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
	<link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon">
	<link rel="icon" href="images/favicon.ico" type="image/x-icon">
</head>
<body>
	<div id="navigation_drawer">
		<div id="drawer_header">
			<img src="images/icon.png" width="20px" style="margin: 0px 5px 0px 0px"/>
			<font>Stigg</font>
		</div>
		<div style="padding: 10px 0px 10px 0px; overflow-y: scroll; height:75%">
		<?
			// Connect to the database
			require_once("../api/util/database.php");
			$db = new DBConnect("localhost", "root", "", "database_guard");
			$con = $db->openConnection();

			$user_query = $db->makeQuery($con, "SELECT user_id, assigned_object_id, name, photo FROM user");

			$first_user_id = null;
			while ($user_result = $user_query->fetch_assoc()){
				if ($first_user_id == null){
					$first_user_id = $user_result["user_id"];
				}
				$assigned_object_id = $user_result["assigned_object_id"];
				// Find the assigned object of the current guard in the database
				$assigned_object_query = $db->makeQuery($con, "SELECT title FROM assigned_object
					WHERE assigned_object_id='$assigned_object_id'");
				$assigned_object_result = $assigned_object_query->fetch_assoc();
				if ($assigned_object_result != null){
					$title = $assigned_object_result["title"];
				} else {
					$title = "Not assigned";
				}
				?>

				<?
				$id = $_GET["user"];
				if ($id == null){
					$id = $first_user_id;
				}
				if ($user_result["user_id"] != $id) {
					echo "<a href='?user=".$user_result['user_id']."' class='user'>";
				}?>
					<table style="width: 100%" cellspacing="0" cellpadding="0" class="<?
						
						echo $user_result["user_id"] == $id ? "user_wrapper_selected" : "user_wrapper"?>
						">
						<div>
							<td style="padding: 15px 10px 15px 20px">
								<img class="user_photo" src="<? echo $user_result["photo"] == null ? "images/avatar_placeholder.png" : $user_result["photo"]?>"/>
							</td>
							<td class='user_info' style="padding: 10px 20px 10px 10px">
								<div><? echo $user_result["name"]?></div>
								<div class='user_assigned_object'><? echo $title?></div>
							<td>
						</div>
					</table>
				<?if ($user_result["user_id"] != $id) {
					echo "</a>";
				}?>
				<?
			}
		?>
		</div>
		<div style="padding: 10px 0px; position: absolute; bottom: 0; left: 0; width: 100%">
			<a href="places.php" style="text-decoration: none">
				<div class="menu_item">Manage Guard Sites</div>
			</a>
			<a href="shifts.php" style="text-decoration: none">
				<div class="menu_item">Assign Shifts</div>
			</a>
		</div>
	</div>
	<div id="content">
		<div id="content_header">Dashboard
			<div style="float: right">
				<form class="content_date" method="post" id="data_form">
					<font style="margin: 0px 10px">View data from</font>
					<input type="date" name="start_date" id="start_date" value="<?echo date('Y-m-d', $start_date)?>" max="<?echo date('Y-m-d', $today_start_ts-60*60*24)?>" oninput="myFunction()"/>
					<font style="margin: 0px 10px">to</font>
					<input type="date" name="end_date" value="<?echo date('Y-m-d', $end_date)?>" max="<?echo date('Y-m-d', $today_start_ts)?>"/>
				</form>
			</div>
		</div>
		<table id="content_table">
			<tr>
				<td class="block">
					<table style="width: 100%">
						<tr>
							<td>
								<?

								$id = $_GET["user"];
								if ($id == null){
									$id = $first_user_id;
								}

								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								// Retrieve location data
								// TODO: Limit to specified period
								$location_query = $db->makeQuery($con, "SELECT latitude, longitude FROM location WHERE user_id='$id' AND time>'$start_date' AND time<'$end_date' ORDER BY time ASC");
								$last_latitude = null;
								$last_longitude = null;
								$locations = array();
								while ($location_result = $location_query->fetch_assoc()){
									$last_latitude = $location_result["latitude"];
									$last_longitude = $location_result["longitude"];
									array_push($locations, array("latitude" => $last_latitude, "longitude" => $last_longitude));
								}
								$locations = json_encode($locations);
								
								if ($last_latitude==null || $last_longitude==null){
									?><h2>No Location History</h2><?
									$last_latitude = 56.662068;
									$last_longitude = -111.339354;
								} else {
									?><h2>Location History</h2><?
								}
								?>
							</td>
						</tr>
						<tr>
							<td>
								<div id="map" class="block_body"></div>
							</td>
						</tr>
					</table>
				</td>
				<td class="block">
					<table style="width: 100%">
						<tr>
							<td>
								<?

								$id = $_GET["user"];
								if ($id == null){
									$id = $first_user_id;
								}

								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								// Retrieve steps data
								// TODO: Limit to specified period
								$steps_query = $db->makeQuery($con, "SELECT time, value FROM step WHERE user_id='$id' AND time>'$today_start_ts' ORDER BY time ASC");
								$yesterday_steps_query = $db->makeQuery($con, "SELECT time, value FROM step WHERE user_id='$id' AND time>'$yesterday_start_ts' AND time<'$today_start_ts' ORDER BY time ASC");
								$day_1_ago_steps_query = $db->makeQuery($con, "SELECT time, value FROM step WHERE user_id='$id' AND time>'$day_1_ago_start_ts' AND time<'$yesterday_start_ts' ORDER BY time ASC");
								$day_2_ago_steps_query = $db->makeQuery($con, "SELECT time, value FROM step WHERE user_id='$id' AND time>'$day_2_ago_start_ts' AND time<'$day_1_ago_start_ts' ORDER BY time ASC");
								$day_3_ago_steps_query = $db->makeQuery($con, "SELECT time, value FROM step WHERE user_id='$id' AND time>'$day_3_ago_start_ts' AND time<'$day_2_ago_start_ts' ORDER BY time ASC");
								$day_4_ago_steps_query = $db->makeQuery($con, "SELECT time, value FROM step WHERE user_id='$id' AND time>'$day_4_ago_start_ts' AND time<'$day_3_ago_start_ts' ORDER BY time ASC");
								$day_5_ago_steps_query = $db->makeQuery($con, "SELECT time, value FROM step WHERE user_id='$id' AND time>'$day_5_ago_start_ts' AND time<'$day_4_ago_start_ts' ORDER BY time ASC");
								$day_6_ago_steps_query = $db->makeQuery($con, "SELECT time, value FROM step WHERE user_id='$id' AND time>'$day_6_ago_start_ts' AND time<'$day_5_ago_start_ts' ORDER BY time ASC");
								
								if (mysqli_num_rows($steps_query) == 0
										and mysqli_num_rows($yesterday_steps_query) == 0
										and mysqli_num_rows($day_1_ago_steps_query) == 0
										and mysqli_num_rows($day_2_ago_steps_query) == 0
										and mysqli_num_rows($day_3_ago_steps_query) == 0
										and mysqli_num_rows($day_4_ago_steps_query) == 0
										and mysqli_num_rows($day_5_ago_steps_query) == 0
										and mysqli_num_rows($day_6_ago_steps_query) == 0
									){
									?><h2>No Steps</h2><?
								} else {
									if ($steps_result = $steps_query->fetch_assoc()){
										$steps = $steps_result["value"];
									}
									if ($yesterday_steps_result = $yesterday_steps_query->fetch_assoc()){
										$yesterday_steps = $yesterday_steps_result["value"];
									}
									if ($day_1_ago_steps_result = $day_1_ago_steps_query->fetch_assoc()){
										$day_1_ago_steps = $day_1_ago_steps_result["value"];
									}
									if ($day_2_ago_steps_result = $day_2_ago_steps_query->fetch_assoc()){
										$day_2_ago_steps = $day_2_ago_steps_result["value"];
									}
									if ($day_3_ago_steps_result = $day_3_ago_steps_query->fetch_assoc()){
										$day_3_ago_steps = $day_3_ago_steps_result["value"];
									}
									if ($day_4_ago_steps_result = $day_4_ago_steps_query->fetch_assoc()){
										$day_4_ago_steps = $day_4_ago_steps_result["value"];
									}
									if ($day_5_ago_steps_result = $day_5_ago_steps_query->fetch_assoc()){
										$day_5_ago_steps = $day_5_ago_steps_result["value"];
									}
									if ($day_6_ago_steps_result = $day_6_ago_steps_query->fetch_assoc()){
										$day_6_ago_steps = $day_6_ago_steps_result["value"];
									}
									
									?><h2>Steps</h2><?
								}
								?>
							</td>
						</tr>
						<tr>
							<td>
								<div id="steps_chart" class="block_body"></div>
							</td>
						</tr>
					</table>
				</td>
				<td class="block">
					<table style="width: 100%">
						<tr>
							<td>
								<?

								$id = $_GET["user"];
								if ($id == null){
									$id = $first_user_id;
								}

								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								// Retrieve heart rate data
								// TODO: Limit to specified period
								$heart_rate_query = $db->makeQuery($con, "SELECT time, value FROM heart_rate WHERE user_id='$id' AND time>'$start_date' AND time<'$end_date' ORDER BY time ASC");
								
								if (mysqli_num_rows($heart_rate_query) == 0){
									?><h2>No Heart Rate</h2><?
								} else {
									?><h2>Heart Rate</h2><?
								}
								?>
							</td>
						</tr>
						<tr>
							<td>
								<div id="heart_rate_chart" class="block_body">
									<?
									$heart_rate_values = array();
									$previous_time = null;
									$previous_value = null;
									while ($heart_rate_result = $heart_rate_query->fetch_assoc()){
										if ($previous_time!=null and $heart_rate_result["time"] - $previous_time<60*60 and ($previous_value != null and abs($previous_value-$heart_rate_result["value"])<20)) {
											$previous_value = $heart_rate_result["value"];
											continue;
										}
										$previous_time = $heart_rate_result["time"];
										$previous_value = $heart_rate_result["value"];
										
										$date = new DateTime("@".$heart_rate_result["time"]);
										$date->setTimezone(new DateTimeZone('GMT-6'));
										$time = $date->format('h:i A');
										
										array_push($heart_rate_values, array("time" => $time, "value" => $heart_rate_result["value"]));
									}
									$heart_rate_values = json_encode($heart_rate_values);
									?>
								</div>
							</td>
						</tr>
					</table>
				</td>
			</tr>
			
			<tr>
				<td class="block">
					<table style="width: 100%">
						<tr>
							<td>
								<?

								$id = $_GET["user"];
								if ($id == null){
									$id = $first_user_id;
								}

								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								// Retrieve activity data
								// TODO: Limit to specified period
								$activity_query = $db->makeQuery($con, "SELECT time, value FROM activity WHERE user_id='$id' AND time>'$start_date' AND time<'$end_date' ORDER BY time DESC");
								
								if (mysqli_num_rows($activity_query) == 0){
									?><h2>No Activity Today</h2><?
								} else {
									?><h2>Daily Activity</h2><?
								}
								?>
							</td>
						</tr>
						<tr>
							<td>
								<div class="block_body" style="overflow-y: scroll">
									<?
									$previous_value = null;
									while ($activity_result = $activity_query->fetch_assoc()){
										if ($activity_result["value"] == $previous_value) continue;
										$date = new DateTime("@".$activity_result["time"]);
										$date->setTimezone(new DateTimeZone('GMT-6'));   
										echo "<div class='activity'>
											<div class='value'>".$activity_result["value"]."</div>"
											.$date->format('d/m, h:i A ').
										"</div>";
										
										$previous_value = $activity_result["value"];
									}
									?>
								</div>
							</td>
						</tr>
					</table>
				</td>
				<td class="block">
					<table style="width: 100%">
						<tr>
							<td>
								<?

								$id = $_GET["user"];
								if ($id == null){
									$id = $first_user_id;
								}

								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								// Retrieve qr data
								// TODO: Limit to specified period
								$qr_query = $db->makeQuery($con, "SELECT time, value FROM qr WHERE user_id='$id' AND time>'$start_date' AND time<'$end_date' ORDER BY time DESC");
								
								if (mysqli_num_rows($qr_query) == 0){
									?><h2>No Scanned QR Codes</h2><?
								} else {
									?><h2>Scanned QR Codes</h2><?
								}
								?>
							</td>
						</tr>
						<tr>
							<td>
								<div class="block_body" style="overflow-y: scroll">
									<?
									while ($qr_result = $qr_query->fetch_assoc()){
										$date = new DateTime("@".$qr_result["time"]);
										$date->setTimezone(new DateTimeZone('GMT-6'));   
										echo "<div class='activity'>
											<div class='value'>".$qr_result["value"]."</div>"
											.$date->format('d/m, h:i A').
										"</div>";
									}
									?>
								</div>
							</td>
						</tr>
					</table>
				</td>
				<td class="block">
					<table style="width: 100%">
						<tr>
							<td>
								<?

								$id = $_GET["user"];
								if ($id == null){
									$id = $first_user_id;
								}

								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								// Retrieve clock in data
								// TODO: Limit to specified period
								$clock_in_query = $db->makeQuery($con, "SELECT time, shift_id, started FROM clock_in WHERE user_id='$id' AND time>'$start_date' AND time<'$end_date' ORDER BY time DESC");
								
								if (mysqli_num_rows($clock_in_query) == 0){
									?><h2>No Clock Info</h2><?
								} else {
									?><h2>Clock In</h2><?
								}
								?>
							</td>
						</tr>
						<tr>
							<td>
								<div class="block_body" style="overflow-y: scroll">
									<?
									while ($clock_in_result = $clock_in_query->fetch_assoc()){
										$date = new DateTime("@".$clock_in_result["time"]);
										$date->setTimezone(new DateTimeZone('GMT-6'));   
										echo "<div class='activity'>
											<div style='float:right'>".
												($clock_in_result["started"] == "1" ? "<font id='started'>Started</font>" : "<font id='ended'>Ended</font>").
											"</div>"
											.$date->format('d/m, h:i A').
										"</div>";
									}
									?>
								</div>
							</td>
						</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td colspan="3">
					<div id="footer_txt">
						Copyright © 2016 stigg security.com
					</div>
				</td>
			</tr>
		</table>
	</div>
</body>

<script>
function myMap() {
	var myLatLng = {lat: <?echo $last_latitude?>, lng: <?echo $last_longitude?>};
	var mapCanvas = document.getElementById("map");
	var mapOptions = {
		center: new google.maps.LatLng(<?echo $last_latitude?>, <?echo $last_longitude?>), 
		zoom: 17
	}
	var map = new google.maps.Map(mapCanvas, mapOptions);
	if (<?echo $last_latitude?>!='56.662068'){
		var marker = new google.maps.Marker({
			position: myLatLng,
			map: map,
			title: 'Current location'
		});
	}
	

	var flightPlanCoordinates = [];
	var locations = JSON.parse('<?echo $locations?>');
	for(var i in locations){
		flightPlanCoordinates.push({
			lat: parseFloat(locations[i]['latitude']),
			lng: parseFloat(locations[i]['longitude'])
		});
	}
	var flightPath = new google.maps.Polyline({
		path: flightPlanCoordinates,
		geodesic: true,
		strokeColor: '#c52127',
		strokeOpacity: 1.0,
		strokeWeight: 2
	});

	flightPath.setMap(map);
}
</script>

<script type="text/javascript">

	  // Load the Visualization API and the corechart package.
      google.charts.load('current', {'packages':['corechart']});

      // Set a callback to run when the Google Visualization API is loaded.
      google.charts.setOnLoadCallback(drawChart);

      // Callback that creates and populates a data table,
      // instantiates the pie chart, passes in the data and
      // draws it.
      function drawChart() {

        // Create the data table.
        var data = new google.visualization.DataTable();
        data.addColumn('string', 'Day');
        data.addColumn('number', 'Steps');
        data.addRows([
          ['Today', parseFloat(<?echo $steps?>)],
          ['Yesterday', parseFloat(<?echo $yesterday_steps?>)],
          ['2 days ago', parseFloat(<?echo $day_1_ago_steps?>)],
          ['3 days ago', parseFloat(<?echo $day_2_ago_steps?>)],
          ['4 days ago', parseFloat(<?echo $day_3_ago_steps?>)],
          ['5 days ago', parseFloat(<?echo $day_4_ago_steps?>)],
          ['6 days ago', parseFloat(<?echo $day_5_ago_steps?>)]
        ]);

        // Set chart options
        var options = {
				'chartArea': {'width': '70%', 'height': '70%'},
				'colors': ['#c52127'],
				'legend': 'none'
		};

        // Instantiate and draw our chart, passing in some options.
        var chart = new google.visualization.BarChart(document.getElementById('steps_chart'));
        chart.draw(data, options);

        // Create the data table.
        var data = new google.visualization.DataTable();
        data.addColumn('string', 'Day');
        data.addColumn('number', 'Heart Rate (BPM)');
		
		var heart_rate_values = JSON.parse('<?echo $heart_rate_values?>');
		for(var i in heart_rate_values){
			data.addRows([
			  [heart_rate_values[i]['time'], parseFloat(heart_rate_values[i]['value'])]
			]);
		}

        // Set chart options
        var options = {
				'chartArea': {'width': '80%', 'height': '70%'},
				'colors': ['#c52127'],
				'legend': 'none'
		};

        // Instantiate and draw our chart, passing in some options.
        var chart = new google.visualization.AreaChart(document.getElementById('heart_rate_chart'));
        chart.draw(data, options);
      }
	  
	function myFunction(){
		document.getElementById("data_form").submit();
	}
</script>

<script src="https://maps.googleapis.com/maps/api/js?callback=myMap&key=AIzaSyB83ubTR7RbcEqbn60FYt_pFV2Ko4cwG1U"></script>