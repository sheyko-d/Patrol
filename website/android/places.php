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
	<script type="text/javascript" src='http://maps.google.com/maps/api/js?libraries=places&key=AIzaSyB83ubTR7RbcEqbn60FYt_pFV2Ko4cwG1U'></script>
	<script src="libs/locationpicker/src/locationpicker.jquery.js"></script>
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
			<div class="menu_item_selected">Manage Guard Sites</div>
			<a href="shifts.php" style="text-decoration: none">
				<div class="menu_item">Assign Shifts</div>
			</a>
		</div>
	</div>
	<div id="content">
		<div id="content_header">Manage Guard Sites</div>
			<table id="content_table_places_left" style="float:left; width:50%; padding-right:0">
				<tr>
					<td class="block" style="padding:0px 0px 15px 0px">
						<table style="width: 100%"  cellspacing="0" cellpadding="0">
							<tr>
								<td>
									<h2 style="padding: 20px 20px 0px 20px; margin-bottom:15px">Sites</h2>
								</td>
							</tr>
							<?
								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								$assigned_object_query = $db->makeQuery($con, "SELECT assigned_object_id, title, latitude, longitude, contacts, safety, video, sitting_duration, watch_removed_max_min, leave_watch_message, admin_emails FROM assigned_object");

								while ($assigned_object_result = $assigned_object_query->fetch_assoc()){
									$assigned_object_id = $assigned_object_result["assigned_object_id"];
									$title = $assigned_object_result["title"];		
									$latitude = $assigned_object_result["latitude"];
									$longitude = $assigned_object_result["longitude"];
									$contacts = $assigned_object_result["contacts"];
									$safety = $assigned_object_result["safety"];
									$video = $assigned_object_result["video"];
									$sitting_duration = $assigned_object_result["sitting_duration"];
									$watch_removed_max_min = $assigned_object_result["watch_removed_max_min"];
									$leave_watch_message = $assigned_object_result["leave_watch_message"];
									$admin_emails = $assigned_object_result["admin_emails"];
									$params = json_encode(array("assigned_object_id"=>$assigned_object_id, "title"=>$title, "latitude"=>$latitude, "longitude"=>$longitude, "contacts"=>$contacts, "safety"=>$safety, "video"=>$video, "sitting_duration"=>$sitting_duration, "watch_removed_max_min"=>$watch_removed_max_min,
																"leave_watch_message"=>$leave_watch_message, "admin_emails"=>$admin_emails));
									$assigned_users_query = $db->makeQuery($con, "SELECT user_id FROM user WHERE assigned_object_id='$assigned_object_id'");
									?>								
									<tr class="table_row">
										<td class="table_cell">
											<? echo $assigned_object_result["title"] ?>
										</td>
										<td style="padding-right:5px">
											<a onClick='editPlace(<? echo $params ?>)'>
												<img src="images/action_edit.png" class="action_img" title="Edit site" />
											</a>
										</td>
										<td>
											<a onClick="deletePlace(this, <? echo $assigned_object_result["assigned_object_id"].", ".mysqli_num_rows($assigned_users_query) ?>)">
												<img src="images/action_delete.png" class="action_img" title="Delete" />
											</a>
										</td>
									</tr>
									<?
								}
							?>
						</table>
					</td>
				</tr>
			</table>
			<table id="content_table_places_right" style="float:right; width:50%; padding-left:0">
				<tr>
					<td class="block" style="padding:0px 0px 10px 0px">
						<table style="width: 100%" cellspacing="0" cellpadding="0">
							<tr>
								<td>
									<h2 style="padding: 20px 20px 0px 20px; margin-bottom:15px">Users</h2>
								</td>
							</tr>
							<?
								// Connect to the database
								require_once("../api/util/database.php");
								$db = new DBConnect("localhost", "root", "", "database_guard");
								$con = $db->openConnection();

								$user_query = $db->makeQuery($con, "SELECT user_id, assigned_object_id, name, photo FROM user");

								while ($user_result = $user_query->fetch_assoc()){
									$assigned_object_id = $user_result["assigned_object_id"];
									$assigned_object_query = $db->makeQuery($con, "SELECT title FROM assigned_object WHERE assigned_object_id='$assigned_object_id'");
									$assigned_object_result = $assigned_object_query->fetch_assoc();
									?>								
									<tr class="table_row">
										<td class="table_cell" style="width:100%">
											<? echo $user_result["name"] ?>
											<img src="images/link.png" class="link_img" title="is assigned to"/>

											<? echo $assigned_object_id!=null ? $assigned_object_result["title"] : "<font style='color: #757575'>Not assigned</font>" ?>
										</td>
										
										<? if ($assigned_object_id==null) {?>
											<td></td>											
											<td>											
												<a onClick="assignUser(<? echo $user_result["user_id"] ?>)">
													<img src="images/action_add.png" class="action_img" title="Assign user" />
												</a>
											</td>
										<? } else {?>										
											<td style="padding-right:20px">
												<a onClick="assignUser(<? echo $user_result["user_id"] ?>)">
													<img src="images/action_edit.png" class="action_img" title="Assign to another site" />
												</a>
											</td>
											<td style="margin-right:0px;padding-right:20px">											
												<a onClick="unassignUser(this, <? echo $user_result["user_id"] ?>)">
													<img src="images/action_delete.png" class="action_img" title="Unassign user" />
												</a>
											</td>
										<? }?>
									</tr>
									<?
								}
							?>
						</table>
					</td>
					<td width="20px"></td>
				</tr>
			</table>
		</table>
	</div>
	<div id="picker_dialog" style="background:rgba(55, 55, 55, 0.7); width:100%; height:100%; position:absolute; top:0; left:0; display: none; align-items: center; ">
		<div style="margin:auto; width:500px;" id="places_list_dialog">
			
			<div style="background:#ffffff; height:auto" id="places_list_dialog">
				<h2 style="padding: 20px 20px 0px 20px; margin-bottom:15px; width:500px">Pick a Place to Assign</h2>
				<table style="width: 100%; padding-right: 15px" cellspacing="0" cellpadding="0">
					<?
					$assigned_object_query = $db->makeQuery($con, "SELECT assigned_object_id, title, latitude, longitude FROM assigned_object");

					while ($assigned_object_result = $assigned_object_query->fetch_assoc()){
						$assigned_object_id = $assigned_object_result["assigned_object_id"];
						$assigned_object_title = $assigned_object_result["title"];
						$assigned_object_latitude = $assigned_object_result["latitude"];
						$assigned_object_longitude = $assigned_object_result["longitude"];
						?>								
						<tr class="table_row">
							<td class="table_cell">
								<? echo $assigned_object_title ?>
							</td>
							<td>
								<a onClick="assignExisting(<?echo $assigned_object_id?>, '<?echo $assigned_object_title?>', '<?echo $assigned_object_latitude?>', '<?echo $assigned_object_longitude?>')">
									<img src="images/link.png" class="action_img" title="Assign to this place" />
								</a>
							</td>
						</tr>
					<?
					}
					?>
				</table>
				<div style="background:#ffffff; padding:10px; height:30px">
					<div style="float:right">
						<input type="submit" value="Cancel" class="dialog_button" onClick="hideDialog()"/>
						<input type="submit" value="Add new place" class="dialog_button" onClick="addNewPlace()"/>
					</div>
				</div>
			</div>
		</div>
		<div style="margin:auto; width:500px; display:none" id="places_map_dialog">
			<div class="edit_place_form">
				<input placeholder="Enter site address…" type="text" id="location_picker_address" style="font-size:20px;padding:20px;"/>
			</div>
			<div id="location_picker" style="width:500px; height:200px;background:#eee"></div>
			<div class="edit_place_form" style="padding: 10px 0">
				<input placeholder="Site specific contacts…" type="text" id="contacts"/>
				<input placeholder="Safety info…" type="text" id="safety"/>
				<input placeholder="Orientation video link…" type="text" id="video"/>
				<input placeholder="Max sitting duration (default: 30 min)…" type="number" id="sitting_duration"/>
				<input placeholder="Max removed watch duration (default: 5 min)…" type="number" id="watch_removed_max_min"/>
				<input placeholder="Reminder to leave watch on site (optional)…" type="text" id="leave_watch_message"/>
				<input placeholder="Admin emails (comma separated)…" type="text" id="admin_emails"/>
			</div>
			<div style="background:#ffffff; padding:10px; height:30px">
				<div style="float:right">
					<input type="submit" value="Cancel" class="dialog_button" onClick="hideDialog()"/>
					<input type="submit" value="Save" class="dialog_button" onClick="saveAssignedObject()"/>
				</div>
			</div>
		</div>
	</div>
</body>

<script>
	function deletePlace(elm, assignedObjectId, usersCount){
		if (usersCount > 0){			
			if (usersCount==1){
				if (!confirm("Are you sure? "+usersCount+" user is assigned to this place!")){
					return;
				}
			} else {				
				if (!confirm("Are you sure? "+usersCount+" users are assigned to this place!")){
					return;
				}
			}
		} else if (!confirm("Are you sure? Nobody is assigned here yet.")){
			return;
		}
		$.ajax({
            url: 'http://stigg.ca/api/assigned_object/delete_assigned_object.php',
            type: 'POST',
			data: {assigned_object_id: assignedObjectId},
            success: function (data) {
                $(elm).parent().parent().remove();
				
				// Reload the page to update the sidebar with assigned assigned objects
				if (usersCount > 0){
					location.reload();
				}
            }
        });
	}
	
	function unassignUser(elm, userId){
		if (!confirm("Are you sure?")){
			return;
		}
		$.ajax({
            url: 'http://stigg.ca/api/assigned_object/unassign_user.php',
            type: 'POST',
			data: {user_id: userId},
            success: function (data) {				
				// Reload the page to update the sidebar with assigned assigned objects
				location.reload();
            }
        });
	}
	
	var latitude = 56.662068;
	var longitude = -111.339354;
	var assignedUserId;
	function assignUser(userId){
		assignedUserId = userId;
		
		$("#picker_dialog").fadeIn().css("display", "flex");
		$("#places_list_dialog").show();
		$("#places_map_dialog").hide();
	}
	
	function assignExisting(assignedObjectId, title, latitude, longitude){
		
		$.ajax({
            url: 'http://stigg.ca/api/assigned_object/assign_user.php',
            type: 'POST',
			data: {
				id: assignedUserId,
				title: title,
				latitude: parseFloat(latitude),
				longitude: parseFloat(longitude)
			},
            success: function (data) {
				alert("User is assigned.");
				
				// Reload the page to update the sidebar with assigned objects
				location.reload();
            }
        });
	}
	
	function addNewPlace(){
		assigned_object_id = null;
		$("#places_list_dialog").hide();
		$("#places_map_dialog").show();
		$('#location_picker').locationpicker({
			location: {latitude: 56.662068, longitude: -111.339354},
			radius: 0,		
			inputBinding: {
				locationNameInput: $('#location_picker_address'),
			},
			enableAutocomplete: true,
			onchanged: function (currentLocation, radius, isMarkerDropped) {
				latitude = currentLocation.latitude;
				longitude = currentLocation.longitude;
			}
		});
	}
	
	function editPlace(params){
		assignedUserId = null;
		assigned_object_id = params.assigned_object_id;
		$("#picker_dialog").fadeIn().css("display", "flex");
		$("#places_list_dialog").hide();
		$("#places_map_dialog").show();
		$('#location_picker_address').val(params.title);
		function overrideTitle() {     
			$('#location_picker_address').val(params.title);
		}

		setTimeout(overrideTitle, 500)
		$('#contacts').val(params.contacts);
		$('#safety').val(params.safety);
		$('#video').val(params.video);
		$('#sitting_duration').val(params.sitting_duration);
		$('#watch_removed_max_min').val(params.watch_removed_max_min);
		$('#leave_watch_message').val(params.leave_watch_message);
		$('#admin_emails').val(params.admin_emails);
		$('#location_picker').locationpicker({
			location: {latitude: params.latitude, longitude: params.longitude},
			radius: 0,		
			inputBinding: {
				locationNameInput: $('#location_picker_address'),
			},
			enableAutocomplete: true,
			onchanged: function (currentLocation, radius, isMarkerDropped) {
				latitude = currentLocation.latitude;
				longitude = currentLocation.longitude;
			}
		});
	}
	
	function saveAssignedObject(){
		var title = $('#location_picker_address').val();
		var contacts = $('#contacts').val();
		var safety = $('#safety').val();
		var video = $('#video').val();
		var sitting_duration = $('#sitting_duration').val();
		var watch_removed_max_min = $('#watch_removed_max_min').val();
		var leave_watch_message = $('#leave_watch_message').val();
		var admin_emails = $('#admin_emails').val();
		
		$.ajax({
            url: 'http://stigg.ca/api/assigned_object/assign_user.php',
            type: 'POST',
			data: {
				id: assignedUserId,
				assigned_object_id: assigned_object_id,
				title: title,
				contacts: contacts,
				safety: safety,
				video: video,
				sitting_duration: sitting_duration,
				watch_removed_max_min: watch_removed_max_min,
				leave_watch_message: leave_watch_message,
				admin_emails: admin_emails,
				latitude: latitude,
				longitude: longitude
			},
            success: function (data) {
				if (assigned_object_id==null){					
					alert("User is assigned.");
				} else {
					alert("Site is updated.");
				}
				
				// Reload the page to update the sidebar with assigned objects
				location.reload();
            }
        });
	}
	
	function hideDialog(){
		$('#picker_dialog').fadeOut();
	}
</script>