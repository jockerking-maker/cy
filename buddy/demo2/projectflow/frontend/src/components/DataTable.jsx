import React, { useState, useMemo } from 'react';

export default function DataTable({
  columns = [],
  data = [],
  loading = false,
  error = null,
  pagination = null,
  onPageChange,
  onSortChange,
  onSearch,
  onFilter,
  onEdit,
  onDelete,
  onSelectionChange,
  searchPlaceholder = '搜索...',
  filters = [],
  batchActions = null,
  emptyText = '暂无数据',
  emptySubtext = '',
  rowKey = 'id',
}) {
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [sortField, setSortField] = useState('');
  const [sortDirection, setSortDirection] = useState('asc');
  const [searchValue, setSearchValue] = useState('');
  const [filterValues, setFilterValues] = useState({});

  // Handle sort
  const handleSort = (key) => {
    let direction = 'asc';
    if (sortField === key) {
      direction = sortDirection === 'asc' ? 'desc' : 'asc';
    }
    setSortField(key);
    setSortDirection(direction);
    if (onSortChange) {
      onSortChange(key, direction);
    }
  };

  // Handle search
  const handleSearch = (value) => {
    setSearchValue(value);
    if (onSearch) {
      onSearch(value);
    }
  };

  // Handle filter
  const handleFilter = (key, value) => {
    const newFilters = { ...filterValues, [key]: value };
    setFilterValues(newFilters);
    if (onFilter) {
      onFilter(newFilters);
    }
  };

  // Handle selection
  const handleSelectAll = (checked) => {
    const newSelected = checked ? new Set(data.map((row) => row[rowKey])) : new Set();
    setSelectedIds(newSelected);
    if (onSelectionChange) {
      onSelectionChange(Array.from(newSelected));
    }
  };

  const handleSelectOne = (id, checked) => {
    const newSelected = new Set(selectedIds);
    if (checked) {
      newSelected.add(id);
    } else {
      newSelected.delete(id);
    }
    setSelectedIds(newSelected);
    if (onSelectionChange) {
      onSelectionChange(Array.from(newSelected));
    }
  };

  const allSelected = data.length > 0 && data.every((row) => selectedIds.has(row[rowKey]));
  const someSelected = selectedIds.size > 0 && !allSelected;

  // Pagination helpers
  const totalPages = pagination?.totalPages || 1;
  const currentPage = pagination?.page || 1;
  const total = pagination?.total || 0;

  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;
    let start = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible - 1);
    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  };

  if (loading) {
    return (
      <div className="data-table-wrapper">
        <div className="loading-center">
          <div className="spinner spinner-lg"></div>
          <span>加载中...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="data-table-wrapper">
        <div className="error-state">
          <div className="error-state-icon">!</div>
          <div className="error-state-text">{error}</div>
          <button className="btn btn-primary" onClick={() => window.location.reload()}>
            重新加载
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="data-table-wrapper">
      {/* Toolbar */}
      <div className="data-table-toolbar">
        <div className="data-table-toolbar-left">
          {onSearch && (
            <div className="data-table-search">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
              <input
                type="text"
                placeholder={searchPlaceholder}
                value={searchValue}
                onChange={(e) => handleSearch(e.target.value)}
              />
            </div>
          )}
          {filters.map((filter) => (
            <select
              key={filter.key}
              className="form-select"
              style={{ width: 'auto', minWidth: '140px' }}
              value={filterValues[filter.key] || ''}
              onChange={(e) => handleFilter(filter.key, e.target.value)}
            >
              <option value="">{filter.placeholder || `全部${filter.label}`}</option>
              {filter.options.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          ))}
        </div>
        <div className="data-table-toolbar-right">
          {selectedIds.size > 0 && batchActions && (
            <div className="batch-actions">
              <span className="batch-info">已选 {selectedIds.size} 项</span>
              {batchActions(Array.from(selectedIds), () => setSelectedIds(new Set()))}
            </div>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              {onSelectionChange && (
                <th className="checkbox-cell">
                  <div className="checkbox-wrapper">
                    <input
                      type="checkbox"
                      checked={allSelected}
                      indeterminate={someSelected ? 'true' : undefined}
                      onChange={(e) => handleSelectAll(e.target.checked)}
                    />
                  </div>
                </th>
              )}
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={sortField === col.key ? 'sorted' : ''}
                  onClick={() => col.sortable !== false && onSortChange && handleSort(col.key)}
                  style={col.sortable === false ? { cursor: 'default' } : {}}
                >
                  {col.label}
                  {col.sortable !== false && onSortChange && (
                    <span className="sort-icon">
                      {sortField === col.key
                        ? sortDirection === 'asc'
                          ? '\u25B2'
                          : '\u25BC'
                        : '\u25B4\u25BE'}
                    </span>
                  )}
                </th>
              ))}
              {(onEdit || onDelete) && <th style={{ width: '120px' }}>操作</th>}
            </tr>
          </thead>
          <tbody>
            {data.length === 0 ? (
              <tr>
                <td
                  colSpan={
                    columns.length + (onSelectionChange ? 1 : 0) + ((onEdit || onDelete) ? 1 : 0)
                  }
                >
                  <div className="empty-state">
                    <div className="empty-state-icon">
                      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                        <path d="M13 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V9z" />
                        <polyline points="13 2 13 9 20 9" />
                      </svg>
                    </div>
                    <div className="empty-state-text">{emptyText}</div>
                    {emptySubtext && <div className="empty-state-subtext">{emptySubtext}</div>}
                  </div>
                </td>
              </tr>
            ) : (
              data.map((row) => (
                <tr key={row[rowKey]}>
                  {onSelectionChange && (
                    <td className="checkbox-cell">
                      <div className="checkbox-wrapper">
                        <input
                          type="checkbox"
                          checked={selectedIds.has(row[rowKey])}
                          onChange={(e) => handleSelectOne(row[rowKey], e.target.checked)}
                        />
                      </div>
                    </td>
                  )}
                  {columns.map((col) => (
                    <td key={col.key}>
                      {col.render ? col.render(row[col.key], row) : row[col.key]}
                    </td>
                  ))}
                  {(onEdit || onDelete) && (
                    <td>
                      <div className="toolbar-actions">
                        {onEdit && (
                          <button
                            className="action-btn"
                            onClick={() => onEdit(row)}
                            title="编辑"
                          >
                            编辑
                          </button>
                        )}
                        {onDelete && (
                          <button
                            className="action-btn danger"
                            onClick={() => onDelete(row)}
                            title="删除"
                          >
                            删除
                          </button>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {pagination && totalPages > 1 && (
        <div className="data-table-pagination">
          <div className="pagination-info">
            共 {total} 条，第 {currentPage}/{totalPages} 页
          </div>
          <div className="pagination-controls">
            <button
              className="pagination-btn"
              disabled={currentPage <= 1}
              onClick={() => onPageChange && onPageChange(1)}
            >
              首页
            </button>
            <button
              className="pagination-btn"
              disabled={currentPage <= 1}
              onClick={() => onPageChange && onPageChange(currentPage - 1)}
            >
              上一页
            </button>
            {getPageNumbers().map((page) => (
              <button
                key={page}
                className={`pagination-btn ${page === currentPage ? 'active' : ''}`}
                onClick={() => onPageChange && onPageChange(page)}
              >
                {page}
              </button>
            ))}
            <button
              className="pagination-btn"
              disabled={currentPage >= totalPages}
              onClick={() => onPageChange && onPageChange(currentPage + 1)}
            >
              下一页
            </button>
            <button
              className="pagination-btn"
              disabled={currentPage >= totalPages}
              onClick={() => onPageChange && onPageChange(totalPages)}
            >
              末页
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
