/* https://jira.codenvycorp.com/browse/CODENVY-446 */
var createPermissions = function(){
  var organizationDb = db.getSiblingDB("organization");
  var workersCollection = organizationDb.getCollection("workers");
  var workspaceIterator = organizationDb.getCollection("workspaces2").find();
  var workersCount = 0;
  while (workspaceIterator.hasNext()) {
    var workspace = workspaceIterator.next();

    if (workersCollection.findOne({"user": workspace.namespace, "workspace": workspace._id}) == null) {
      workersCollection.update({
        "user": workspace.namespace,
        "workspace": workspace._id
      },
      {
        "user": workspace.namespace,
        "workspace": workspace._id,
        "actions" : ["read", "run", "use", "configure", "setPermissions", "readPermissions", "delete"]
      },
      {
        upsert: true
      });
      workersCount++;
    }
  }

  if (workersCount == 0) {
    print("All workspaces owners have permissions");
  } else {
    print("Permissions for " + workersCount +" workspaces owners have been created");
  }
}

createPermissions();

// ---------------------------------------------
var checkActions = function(actions, objectType, id) {
  var result = [];
  for (var i = 0; i < actions.length; i++) {
    var action = actions[i];
    if (action == "search") {
      //ignore
    } else if (action != "read" && action != "update" && action != "delete") {
      print("Unknown action " + action + " in " + objectType + " with id " + id + ". It'll be ignored.");
    } else {
      result.push(action);
    }
  }
  return result;
};

var createAcl = function(collectionName, objectType){
  var organizationDb = db.getSiblingDB("organization");
  var collection = organizationDb.getCollection(collectionName);
  var updateNumber = 0;

  var objectIterator = collection.find();
  while (objectIterator.hasNext()) {
    var object = objectIterator.next();
    var permissions = object.permissions;

    var acl = [];

    if(permissions == null || permissions == undefined) {
      continue;
    }

    permissions.groups.forEach(function(group) {
      if (group.name != "public") {
        print("Unknown group name " + group.name + " in "+objectType+" with id "
        + object._id+". It will be ignored.");
      } else {
        acl.push({"user":"*", "actions": checkActions(group.acl)});
      }
    });

    for (var user in permissions.users) {
      var actions = checkActions(permissions.users[user]);
      acl.push({"user": user, "actions": actions});
    }

    delete object.permissions;
    object.acl = acl;

    collection.update({"_id" : object._id}, object);
    updateNumber++;
  }

  print("Added acl for " + updateNumber + " " + collectionName + ". Total number " + collection.count());
}

createAcl("recipes", "recipe");
createAcl("stacks", "stack");

// ---------------------------------------------
var updateOrganization = function(){
    var organizationDb = db.getSiblingDB("organization");
    var ssh = organizationDb.getCollection("ssh");

    ssh.dropIndex( { "owner": 1, "service": 1, "name": 1 } )
    ssh.update({service:"git"}, {$set : {service:"vcs"}}, {multi:true});

    var docs = ssh.aggregate([
      { $group: {
        _id: { owner: "$owner", service: "$service", name: "$name" },
        uniqueIds: { $addToSet: "$_id" },
        count: { $sum: 1 }
      }},
      { $match: {
        count: { $gt: 1 }
      }}
    ]);

    var removedDups = 0;
    while (docs.hasNext()) {
        var doc = docs.next();
        for (j = 1; j <  doc.uniqueIds.length; j++) {
            ssh.remove({_id:doc.uniqueIds[j]});
            removedDups++;
        }
    }
    print("Removed dups: " + removedDups);
    ssh.createIndex({ "owner": 1, "service": 1, "name": 1 }, {unique:true})
}
updateOrganization();


/* https://jira.codenvycorp.com/browse/CODENVY-528 */
var createAdminsPermissions = function(admins){
  var organizationDb = db.getSiblingDB("organization");
  var permissionsCollection = organizationDb.getCollection("permissions");

  var systemDomain = "system";
  var instance = null; // system domain support only nullable instance
  var adminsActions = ["setPermissions", "manageUsers", "manageCodenvy"];
  var count = 0;
  print("Starting adding system permissions for admins.");
  print("System permissions: [" + adminsActions + "].");
  admins.forEach(function(admin) {
    try {
      permissionsCollection.update({"user": admin,
      "domain": systemDomain,
      "instance": instance},
      {"user": admin,
      "domain": systemDomain,
      "instance": instance,
      "actions":adminsActions},
      {"upsert":true});
      count++;
      print("Added system permissions for admin with id " + admin + ". ");
    }
    catch(err) {
      print("Can't add system permissions for admin with id " + admin + ". " + err.message);
    }
  });
  print("System permissions for " + count + " admins successfully created.")
}

var ADMINS=[];
createAdminsPermissions(ADMINS);