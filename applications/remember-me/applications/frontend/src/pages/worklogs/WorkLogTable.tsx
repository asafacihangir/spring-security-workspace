import { Link } from 'react-router-dom'
import type { WorkLog } from '../../services/worklogs'

export default function WorkLogTable({ workLogs }: { workLogs: WorkLog[] }) {
  if (workLogs.length === 0) {
    return <p>No work logs.</p>
  }
  return (
    <table className="table table-striped">
      <thead>
        <tr>
          <th>Created</th>
          <th>Created By</th>
          <th>Explanation</th>
        </tr>
      </thead>
      <tbody>
        {workLogs.map((workLog) => (
          <tr key={workLog.id}>
            <td>{workLog.createdDate}</td>
            <td>{workLog.createdBy}</td>
            <td>
              <Link to={`/work-logs/${workLog.id}`}>{workLog.explanation}</Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
