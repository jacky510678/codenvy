source commons.sh
$pigDir/pig $runMode -param log=$log -param fromDate=$fromDate -param toDate=$toDate $scriptDir/event-count-workspace-destroyed.pig