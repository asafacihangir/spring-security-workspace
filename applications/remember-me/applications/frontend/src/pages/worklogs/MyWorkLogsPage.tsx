import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyWorkLogs, type WorkLog } from '../../services/worklogs'
import { useAuth } from '../../hooks/useAuth'
import WorkLogTable from './WorkLogTable'

export default function MyWorkLogsPage() {
  const { user } = useAuth()
  const [workLogs, setWorkLogs] = useState<WorkLog[] | null>(null)

  useEffect(() => {
    getMyWorkLogs().then(setWorkLogs).catch(() => setWorkLogs([]))
  }, [])

  return (
    <>
      <h1>My Work Logs</h1>
      <p>Below you can find the work logs for {user?.email}.</p>
      {workLogs === null ? <p>Loading…</p> : <WorkLogTable workLogs={workLogs} />}
      <Link className="btn btn-primary" to="/work-logs/new">
        Create Work Log
      </Link>
    </>
  )
}
