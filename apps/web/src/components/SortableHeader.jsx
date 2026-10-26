import { ArrowDownUp } from 'lucide-react';

const SortableHeader = ({ label, sortKey, onSort }) => (
    <th onClick={() => onSort(sortKey)}>
        <div className="sortable-header">
            {label}
            <ArrowDownUp />
        </div>
    </th>
);
export default SortableHeader; // default export
