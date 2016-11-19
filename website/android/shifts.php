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
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
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

				<a href="index.php?user=<? echo $user_result["user_id"]?>" class="user">
					<table style="width: 100%; padding:0" cellspacing="0" cellpadding="0" class="user_wrapper">
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
				</a>
				<?
			}
		?>
		</div>
		<div style="padding: 10px 0px; position: absolute; bottom: 0; left: 0; width: 100%">
			<a href="places.php" style="text-decoration: none">
				<div class="menu_item">Manage Guard Sites</div>
			</a>
			<div class="menu_item_selected">Assign Shifts</div>
		</div>
	</div>
	<div id="content">
		<div id="content_header">Assign shifts</div>
			<table id="content_table_places_left" style="float:left; width:50%; padding-right:0">
				<tr>
					<td class="block" style="padding:0px 0px 10px 0px">
						<table style="width: 100%"  cellspacing="0" cellpadding="0">
							<tr>
								<td>
									<h2 style="padding: 20px 20px 0px 20px; margin-bottom:15px">Shifts</h2>
								</td>
							</tr>
							<?
								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								$shift_query = $db->makeQuery($con, "SELECT shift_id, name, start_time, end_time FROM shift ORDER BY shift_id ASC");

								while ($shift_result = $shift_query->fetch_assoc()){
									$shift_id = $shift_result["shift_id"];
									$shift_name = $shift_result["name"];
									$ts = mktime(0, 0, 0, date("n"), date("j") - date("N") + 1);
									$shift_start_time = $ts + $shift_result["start_time"] + date("Z");
									$shift_end_time = $ts + $shift_result["end_time"] + date("Z");
									$assigned_users_query = $db->makeQuery($con, "SELECT user_id FROM user WHERE user_id IN (SELECT user_id FROM assigned_shift WHERE shift_id='$shift_id')");
									?>								
									<tr class="table_row">
										<td class="table_cell" style="width:100%">
											<? echo $shift_name." <font style='color:#aaa; margin-left:10px'>".gmdate("h:i a", $shift_start_time)." — ".gmdate("h:i a", $shift_end_time)."</font>"  ?>
										</td>
										<td style="padding-right:20px">
											<a onClick="deleteShift(this, <? echo $shift_id.", ".mysqli_num_rows($assigned_users_query) ?>)">
												<img src="images/action_delete.png" class="action_img" title="Delete" />
											</a>
										</td>
									</tr>
									<?
								}
							?>
						</table>
						<div style="padding-bottom:15px;"><h2 style="padding: 20px 20px 0px 20px; margin-bottom:15px">Add new shift</h2>
							<input placeholder="Enter title..." style="text-align: left; margin: 0px 0px 10px 10px; color:#333;  font-family: 'Roboto', sans-serif; font-weight:normal; font-size:15px" id="add_shift_name"/>
							<br>
							<font style="padding:10px 10px 10px 20px; font-family: 'Roboto', sans-serif;">Start time:</font>
							<select id="add_shift_start_weekday" style="padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;">
								<option value="0">Monday</option>
								<option value="86400">Tuesday</option>
								<option value="172800">Wednesday</option>
								<option value="259200">Thursday</option>
								<option value="345600">Friday</option>
								<option value="432000">Saturday</option>
								<option value="518400">Sunday</option>
							</select>
							<select style="margin-left:26px;margin-top:10px; padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;" id="add_shift_start_hour">
							  <option value="0">00</option>
							  <option value="1">01</option>
							  <option value="2">02</option>
							  <option value="3">03</option>
							  <option value="4">04</option>
							  <option value="5">05</option>
							  <option value="6">06</option>
							  <option value="7">07</option>
							  <option value="8" selected>08</option>
							  <option value="9">09</option>
							  <option value="10">10</option>
							  <option value="11">11</option>
							  <option value="12">12</option>
							</select>
							:
							<select style="margin-top:10px; padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;" id="add_shift_start_minute">
							  <option value="0" selected>00</option>
							  <option value="15">15</option>
							  <option value="30">30</option>
							  <option value="45">45</option>
							</select>
							<select style="margin-top:10px; padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;"  id="add_shift_start_am_pm">
							  <option value="0" selected>AM</option>
							  <option value="43200">PM</option>
							</select>
							<br>

							<font style="padding:10px 10px 10px 20px; font-family: 'Roboto', sans-serif;">End time:</font>
							<select id="add_shift_end_weekday" style="padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;">
								<option value="0">Monday</option>
								<option value="86400">Tuesday</option>
								<option value="172800">Wednesday</option>
								<option value="259200">Thursday</option>
								<option value="345600">Friday</option>
								<option value="432000">Saturday</option>
								<option value="518400">Sunday</option>
							</select>
							<select style="margin-left:26px;margin-top:10px; padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;" id="add_shift_end_hour">
							  <option value="0">00</option>
							  <option value="1">01</option>
							  <option value="2">02</option>
							  <option value="3">03</option>
							  <option value="4">04</option>
							  <option value="5" selected>05</option>
							  <option value="6">06</option>
							  <option value="7">07</option>
							  <option value="8">08</option>
							  <option value="9">09</option>
							  <option value="10">10</option>
							  <option value="11">11</option>
							  <option value="12">12</option>
							</select>
							:
							<select style="margin-top:10px; padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;" id="add_shift_end_minute">
							  <option value="0" selected>00</option>
							  <option value="15">15</option>
							  <option value="30">30</option>
							  <option value="45">45</option>
							</select>
							<select style="margin-top:10px; padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;"  id="add_shift_end_am_pm">
							  <option value="0">AM</option>
							  <option value="43200" selected>PM</option>
							</select>
							<br>
							<input type="submit" value="Add" style="width: 60px; margin: 10px 0px 0px 20px; background-color:#c52127; color:#fff; font-size:13sp; height:27px; margin-bottom: 1px" onClick="addNewShift()" />
						</div>
					</td>
				</tr>
			</table>
			<table id="content_table_places_right" style="float:right; width:50%; padding-left:0">
				<tr>
					<td class="block" style="padding:0px 0px 10px 0px">
						<table style="width: 100%" cellspacing="0" cellpadding="0">
							<tr>
								<td>
									<h2 style="padding: 20px 20px 0px 20px; margin-bottom:15px">Already assigned users</h2>
								</td>
							</tr>
							<?
								$assigned_shifts_query = $db->makeQuery($con, "SELECT assigned_shift_id, shift_id, user_id FROM assigned_shift ORDER BY assigned_shift_id ASC");

								while ($assigned_shifts_result = $assigned_shifts_query->fetch_assoc()){
									$user_id = $assigned_shifts_result["user_id"];
									$user_query = $db->makeQuery($con, "SELECT name FROM user WHERE user_id='$user_id'");									
									$user_result = $user_query->fetch_assoc();
									
									$shift_id = $assigned_shifts_result["shift_id"];
									$shift_query = $db->makeQuery($con, "SELECT name FROM shift WHERE shift_id='$shift_id'");							
									$shift_result = $shift_query->fetch_assoc();
									?>								
									<tr class="table_row">
										<td class="table_cell" style="width:100%">
											<? echo $user_result["name"] ?>
											<img src="images/link.png" class="link_img" title="is assigned to"/>

											<? echo $shift_result["name"] ?>
										</td>
										<td style="margin-right:0px;padding-right:20px">										
											<a onClick="unassignUser(this, <? echo $assigned_shifts_result["assigned_shift_id"] ?>)">
												<img src="images/action_delete.png" class="action_img" title="Unassign user" />
											</a>
										</td>
									</tr>
									<?
								}
							?>
						</table>
						<div style="padding-bottom:15px;"><h2 style="padding: 20px 20px 0px 20px; margin-bottom:15px">Assign new user</h2>
							<select style="padding: 4px 8px;  margin: 7px 0 7px 20px; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;" id="assign_user_select">
								<?
								
								$users_query = $db->makeQuery($con, "SELECT user_id, name FROM user") or die(mysqli_error($con));
								while($users_result = $users_query->fetch_assoc()){
								
									echo "<option value=".$users_result["user_id"].">".$users_result["name"]."</option>";
								}
								?>
							</select>
							<font style="padding:10px; font-family: 'Roboto', sans-serif">assign to</font>
							<select id="assign_shift_select" style="padding: 4px 8px;  margin: 7px 0; border: 1px solid #ccc; border-radius: 3px; overflow: hidden; background-color: #fff;">
								<?
								
								$shifts_query = $db->makeQuery($con, "SELECT shift_id, name FROM shift") or die(mysqli_error($con));
								while($shifts_result = $shifts_query->fetch_assoc()){
								
									echo "<option value=".$shifts_result["shift_id"].">".$shifts_result["name"]."</option>";
								}
								?>
							</select>
							<input type="submit" value="Assign" style="margin-left:10px; background-color:#c52127; color:#fff; font-size:13sp; height:27px; margin-bottom: 1px" onClick="assignUser()" />
						</div>
					</td>
					<td width="20px"></td>
				</tr>
			</table>
		</table>
	</div>
</body>

<script>
	function deleteShift(elm, shiftId, usersCount){
		if (usersCount > 0){
			if (usersCount==1){
				if (!confirm("Are you sure? "+usersCount+" user is assigned to this shift!")){
					return;
				}
			} else {				
				if (!confirm("Are you sure? "+usersCount+" users are assigned to this shift!")){
					return;
				}
			}
		} else if (!confirm("Are you sure? Nobody is assigned to this shift yet.")){
			return;
		}
		$.ajax({
            url: 'http://stigg.ca/api/assigned_shift/delete_assigned_shift.php',
            type: 'POST',
			data: {shift_id: shiftId},
            success: function (data) {
                $(elm).parent().parent().remove();
				
				location.reload();
            }
        });
	}
	
	function unassignUser(elm, assigned_shift_id){
		if (!confirm("Are you sure?")){
			return;
		}
		$.ajax({
            url: 'http://stigg.ca/api/assigned_shift/unassign_user.php',
            type: 'POST',
			data: {assigned_shift_id: assigned_shift_id},
            success: function (data) {				
                $(elm).parent().parent().remove();
				
				// Reload the page to update the sidebar with assigned shifts
				location.reload();
            }
        });
	}
	
	var latitude = 56.662068;
	var longitude = -111.339354;
	var assignedUserId;
	function assignUser(){
		var user_id = $("#assign_user_select").val();
		var shift_id = $("#assign_shift_select").val();
		
		$.ajax({
            url: 'http://stigg.ca/api/assigned_shift/assign_user_directly.php',
            type: 'POST',
			data: {
				user_id: user_id,
				shift_id: shift_id
			},
            success: function (data) {				
				// Reload the page to update the sidebar with assigned shifts
				location.reload();
            }
        });
	}
	
	function addNewShift(){
		if ($("#add_shift_name").val() == ""){
			alert("Please enter shift title");
			return;
		}
		
		var name = $("#add_shift_name").val();
		
		var start_time = $("#add_shift_start_hour").val()*60*60;
		var start_time = start_time + $("#add_shift_start_minute").val()*60;
		var start_time = start_time + parseInt($("#add_shift_start_am_pm").val());
		var start_time = start_time + parseInt($("#add_shift_start_weekday").val());
		
		var end_time = $("#add_shift_end_hour").val()*60*60;
		var end_time = end_time + $("#add_shift_end_minute").val()*60;
		var end_time = end_time + parseInt($("#add_shift_end_am_pm").val());
		var end_time = end_time + parseInt($("#add_shift_end_weekday").val());
		
		if (start_time > end_time){
			alert("Invalid shift time selected");
			return;
		}
		
		$.ajax({
            url: 'http://stigg.ca/api/assigned_shift/add_shift.php',
            type: 'POST',
			data: {
				name: name,
				start_time: start_time,
				end_time: end_time
			},
            success: function (data) {
				// Reload the page to update the sidebar with assigned shifts
				location.reload();
            }
        });
	}
	
	function hideDialog(){
		$('#picker_dialog').fadeOut();
	}
</script>