import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../../services/api'
import { getWorkLogs, type WorkLog } from '../../services/worklogs'
import WorkLogTable from './WorkLogTable'

export default function AllWorkLogsPage() {
  const navigate = useNavigate()
  const [workLogs, setWorkLogs] = useState<WorkLog[] | null>(null)

  useEffect(() => {
    getWorkLogs()
      .then(setWorkLogs)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 403) {
          navigate('/403', { replace: true })
        } else {
          setWorkLogs([])
        }
      })
  }, [navigate])

  return (
    <>
      <h1>All Work Logs</h1>
      {workLogs === null ? <p>Loading…</p> : <WorkLogTable workLogs={workLogs} />}
      <Link className="btn btn-primary" to="/work-logs/new">
        Create Work Log
      </Link>
    </>
  )
}
