import React, { useState, useEffect, useMemo } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import {
  ClipboardList,
  Plus,
  Filter,
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  Building2,
  Package,
  Calendar,
  User as UserIcon,
  X,
  FileSpreadsheet,
  RotateCcw,
  Flame,
  Lock,
  UserCheck,
  Check
} from 'lucide-react';

export const AssignmentsPage = () => {
  const { user, role, baseId, baseName } = useAuth();

  // Strict role check: LOGISTICS_OFFICER is entirely blocked from this page
  if (role === 'LOGISTICS_OFFICER') {
    return <Navigate to="/dashboard" replace />;
  }

  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);

  // Lookups
  const [bases, setBases] = useState([]);
  const [equipmentTypes, setEquipmentTypes] = useState([]);
  const [lookupsLoading, setLookupsLoading] = useState(true);

  // Table Data & State
  const [assignments, setAssignments] = useState([]);
  const [tableLoading, setTableLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  // Pagination State
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filter Bar State
  const [filterStartDate, setFilterStartDate] = useState('');
  const [filterEndDate, setFilterEndDate] = useState('');
  const [filterBaseId, setFilterBaseId] = useState(role === 'ADMIN' ? '' : (baseId ? String(baseId) : ''));
  const [filterEquipmentTypeId, setFilterEquipmentTypeId] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  // Assign Asset Modal State
  const [showAddModal, setShowAddModal] = useState(false);
  const [formBaseId, setFormBaseId] = useState(role === 'ADMIN' ? '' : (baseId ? String(baseId) : ''));
  const [formEquipmentTypeId, setFormEquipmentTypeId] = useState('');
  const [formPersonnelName, setFormPersonnelName] = useState('');
  const [formPersonnelIdNo, setFormPersonnelIdNo] = useState('');
  const [formQuantity, setFormQuantity] = useState('');
  const [formAssignedDate, setFormAssignedDate] = useState(todayStr);
  const [formNotes, setFormNotes] = useState('');

  // Action Dialog State (Expend / Return)
  const [actionModal, setActionModal] = useState(null); // { type: 'expend'|'return', assignment: item }
  const [actionDate, setActionDate] = useState(todayStr);
  const [actionNotes, setActionNotes] = useState('');
  const [actionSubmitting, setActionSubmitting] = useState(false);

  // Form Submission & Status Feedback State
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formGeneralError, setFormGeneralError] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);

  // Sync Base ID for Base Commander
  useEffect(() => {
    if (role !== 'ADMIN' && baseId) {
      setFormBaseId(String(baseId));
      setFilterBaseId(String(baseId));
    }
  }, [role, baseId]);

  // Load lookups
  useEffect(() => {
    const fetchLookups = async () => {
      setLookupsLoading(true);
      try {
        const [basesRes, equipRes] = await Promise.all([
          api.get('/api/bases'),
          api.get('/api/equipment-types'),
        ]);

        const basesData = Array.isArray(basesRes.data)
          ? basesRes.data
          : (basesRes.data?.data || []);
        const equipData = Array.isArray(equipRes.data)
          ? equipRes.data
          : (equipRes.data?.data || []);

        setBases(basesData);
        setEquipmentTypes(equipData);
      } catch (err) {
        console.error('Failed to load lookup data:', err);
      } finally {
        setLookupsLoading(false);
      }
    };

    fetchLookups();
  }, []);

  // Fetch paginated assignments
  const fetchAssignments = async () => {
    setTableLoading(true);
    setFetchError(null);

    try {
      const params = {
        page,
        size: pageSize,
        sort: 'assignedDate,desc',
      };

      if (filterStartDate) params.start_date = filterStartDate;
      if (filterEndDate) params.end_date = filterEndDate;

      if (role === 'ADMIN') {
        if (filterBaseId) params.base_id = filterBaseId;
      } else if (baseId) {
        params.base_id = baseId;
      }

      if (filterEquipmentTypeId) params.equipment_type_id = filterEquipmentTypeId;
      if (filterStatus) params.status = filterStatus;

      const res = await api.get('/api/assignments', { params });
      const payload = res.data?.data || res.data;

      setAssignments(payload.content || []);
      setTotalPages(payload.totalPages || 0);
      setTotalElements(payload.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch assignments:', err);
      setFetchError(
        err.response?.data?.error || 'Unable to retrieve custody manifests. Please verify your connection or try again.'
      );
    } finally {
      setTableLoading(false);
    }
  };

  useEffect(() => {
    fetchAssignments();
  }, [page, filterStartDate, filterEndDate, filterBaseId, filterEquipmentTypeId, filterStatus]);

  // Reset form inputs
  const resetForm = () => {
    setFormBaseId(role === 'ADMIN' ? '' : (baseId ? String(baseId) : ''));
    setFormEquipmentTypeId('');
    setFormPersonnelName('');
    setFormPersonnelIdNo('');
    setFormQuantity('');
    setFormAssignedDate(todayStr);
    setFormNotes('');
    setFieldErrors({});
    setFormGeneralError(null);
  };

  // Handle Create Assignment
  const handleSubmitAssignment = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFieldErrors({});
    setFormGeneralError(null);

    const errors = {};
    const effectiveBase = role === 'ADMIN' ? formBaseId : (baseId || formBaseId);
    if (!effectiveBase) {
      errors.baseId = 'Please select a military base';
    }
    if (!formEquipmentTypeId) {
      errors.equipmentTypeId = 'Please select equipment type';
    }
    if (!formPersonnelName.trim()) {
      errors.personnelName = 'Personnel name is required';
    }
    const qtyNum = parseInt(formQuantity, 10);
    if (!qtyNum || qtyNum <= 0) {
      errors.quantity = 'Quantity must be at least 1';
    }
    if (!formAssignedDate) {
      errors.assignedDate = 'Assigned date is required';
    } else if (formAssignedDate > todayStr) {
      errors.assignedDate = 'Assigned date cannot be in the future';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setSubmitting(false);
      return;
    }

    const payload = {
      base_id: Number(effectiveBase),
      equipment_type_id: Number(formEquipmentTypeId),
      personnel_name: formPersonnelName.trim(),
      personnel_id_no: formPersonnelIdNo.trim() || null,
      quantity: qtyNum,
      assigned_date: formAssignedDate,
      notes: formNotes.trim() || null,
    };

    try {
      const res = await api.post('/api/assignments', payload);
      const createdItem = res.data?.data || res.data;

      setToastMessage({
        type: 'success',
        text: `Asset custody record #ASN-${createdItem.id} issued to ${createdItem.personnelName} (${createdItem.quantity}x ${createdItem.equipmentName || 'units'}).`,
      });

      resetForm();
      setShowAddModal(false);

      setTimeout(() => setToastMessage(null), 5000);

      if (page !== 0) {
        setPage(0);
      } else {
        fetchAssignments();
      }
    } catch (err) {
      console.error('Error creating assignment:', err);
      const errData = err.response?.data;

      if (errData?.details && typeof errData.details === 'object') {
        const mappedErrors = {};
        for (const [key, msg] of Object.entries(errData.details)) {
          if (key === 'baseId' || key === 'base_id') mappedErrors.baseId = msg;
          else if (key === 'equipmentTypeId' || key === 'equipment_type_id') mappedErrors.equipmentTypeId = msg;
          else if (key === 'personnelName' || key === 'personnel_name') mappedErrors.personnelName = msg;
          else if (key === 'quantity') mappedErrors.quantity = msg;
          else if (key === 'assignedDate' || key === 'assigned_date') mappedErrors.assignedDate = msg;
          else mappedErrors[key] = msg;
        }
        setFieldErrors(mappedErrors);
      }

      setFormGeneralError(
        errData?.error || 'Failed to issue asset. Please verify input values.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  // Open Action Confirm Dialog
  const openActionModal = (assignment, type) => {
    setActionModal({ type, assignment });
    setActionDate(todayStr);
    setActionNotes('');
  };

  // Submit Expend or Return Action
  const handleSubmitAction = async (e) => {
    e.preventDefault();
    if (!actionModal) return;

    setActionSubmitting(true);
    const { type, assignment } = actionModal;

    const endpoint = type === 'expend'
      ? `/api/assignments/${assignment.id}/expend`
      : `/api/assignments/${assignment.id}/return`;

    const payload = type === 'expend'
      ? { expended_date: actionDate, notes: actionNotes.trim() || null }
      : { returned_date: actionDate, notes: actionNotes.trim() || null };

    try {
      await api.patch(endpoint, payload);

      setToastMessage({
        type: 'success',
        text: type === 'expend'
          ? `Assignment #ASN-${assignment.id} successfully marked as EXPENDED.`
          : `Assignment #ASN-${assignment.id} successfully marked as RETURNED to armory.`,
      });
      setTimeout(() => setToastMessage(null), 5000);

      setActionModal(null);
      fetchAssignments();
    } catch (err) {
      console.error(`Error marking assignment as ${type}:`, err);
      setToastMessage({
        type: 'error',
        text: err.response?.data?.error || `Failed to mark assignment as ${type}.`,
      });
      setTimeout(() => setToastMessage(null), 5000);
    } finally {
      setActionSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'assigned':
        return 'bg-blue-950/60 text-blue-400 border-blue-500/40';
      case 'expended':
        return 'bg-rose-950/60 text-rose-400 border-rose-500/40';
      case 'returned':
        return 'bg-emerald-950/60 text-emerald-400 border-emerald-500/40';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  const clearFilters = () => {
    setFilterStartDate('');
    setFilterEndDate('');
    if (role === 'ADMIN') setFilterBaseId('');
    setFilterEquipmentTypeId('');
    setFilterStatus('');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* Top Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-1.5 rounded-lg bg-amber-950/60 border border-amber-500/30 text-amber-400">
              <ClipboardList className="w-5 h-5" />
            </span>
            <h1 className="text-xl md:text-2xl font-bold tracking-tight text-white">
              Personnel Custody & Expenditures
            </h1>
          </div>
          <p className="text-xs font-mono text-slate-400 mt-1">
            ARMORY ASSIGNMENTS • AMMUNITION & WEAPON EXPENDITURE AUDITING
          </p>
        </div>

        {/* Action Button */}
        <div className="flex items-center space-x-3">
          <button
            id="open-assign-asset-modal"
            onClick={() => {
              setFieldErrors({});
              setFormGeneralError(null);
              setShowAddModal(true);
            }}
            className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs uppercase font-mono tracking-wider transition-all shadow-lg shadow-amber-500/20 active:scale-95"
          >
            <Plus className="w-4 h-4 stroke-[3]" />
            <span>Assign Asset</span>
          </button>
        </div>
      </div>

      {/* Global Toast Notification */}
      {toastMessage && (
        <div
          className={`p-3.5 rounded-xl border flex items-center justify-between text-xs font-mono transition-all animate-fade-in ${
            toastMessage.type === 'success'
              ? 'bg-emerald-950/50 border-emerald-500/40 text-emerald-200'
              : 'bg-rose-950/50 border-rose-500/40 text-rose-200'
          }`}
        >
          <div className="flex items-center space-x-2.5">
            {toastMessage.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            )}
            <span>{toastMessage.text}</span>
          </div>
          <button
            onClick={() => setToastMessage(null)}
            className="text-slate-400 hover:text-white p-1"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Filter Bar */}
      <div className="p-4 rounded-xl bg-[#0c1322] border border-slate-800 shadow-md space-y-3">
        <div className="flex items-center justify-between text-xs font-mono text-slate-400">
          <span className="flex items-center">
            <Filter className="w-3.5 h-3.5 mr-1.5 text-amber-400" />
            FILTER CUSTODY MANIFESTS
          </span>

          {(filterStartDate || filterEndDate || (role === 'ADMIN' && filterBaseId) || filterEquipmentTypeId || filterStatus) && (
            <button
              onClick={clearFilters}
              className="text-[11px] text-slate-400 hover:text-amber-400 underline decoration-dotted"
            >
              Reset Filters
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {/* Start Date */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              Start Date
            </label>
            <input
              type="date"
              id="filter-start-date"
              value={filterStartDate}
              max={todayStr}
              onChange={(e) => {
                setFilterStartDate(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-amber-500/60 focus:outline-none"
            />
          </div>

          {/* End Date */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              End Date
            </label>
            <input
              type="date"
              id="filter-end-date"
              value={filterEndDate}
              max={todayStr}
              onChange={(e) => {
                setFilterEndDate(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-amber-500/60 focus:outline-none"
            />
          </div>

          {/* Base Filter (Visible only to ADMIN) */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              Military Base
            </label>
            {role === 'ADMIN' ? (
              <select
                id="filter-base-select"
                value={filterBaseId}
                onChange={(e) => {
                  setFilterBaseId(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-amber-500/60 focus:outline-none"
              >
                <option value="">All Bases</option>
                {bases.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            ) : (
              <div
                title="Your access is scoped strictly to your assigned base"
                className="w-full px-3 py-2 rounded-lg bg-slate-900/80 border border-slate-800 text-xs text-slate-400 font-mono flex items-center justify-between"
              >
                <span className="truncate">{baseName || 'Assigned Base'}</span>
                <Lock className="w-3.5 h-3.5 text-amber-500/70 ml-1.5 shrink-0" />
              </div>
            )}
          </div>

          {/* Equipment Type Filter */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              Equipment Type
            </label>
            <select
              id="filter-equipment-select"
              value={filterEquipmentTypeId}
              onChange={(e) => {
                setFilterEquipmentTypeId(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-amber-500/60 focus:outline-none"
            >
              <option value="">All Equipment</option>
              {equipmentTypes.map((eq) => (
                <option key={eq.id} value={eq.id}>
                  {eq.name} ({eq.category})
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              Custody Status
            </label>
            <select
              id="filter-status-select"
              value={filterStatus}
              onChange={(e) => {
                setFilterStatus(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-amber-500/60 focus:outline-none"
            >
              <option value="">All Statuses</option>
              <option value="assigned">Assigned (In Field)</option>
              <option value="expended">Expended (Ammunition / Consumed)</option>
              <option value="returned">Returned (Armory Restocked)</option>
            </select>
          </div>
        </div>
      </div>

      {/* History Table Section */}
      <div className="rounded-xl border border-slate-800 overflow-hidden bg-[#0c1322] shadow-xl">
        <div className="p-4 border-b border-slate-800/80 flex items-center justify-between bg-slate-950/40">
          <div className="flex items-center space-x-2">
            <FileSpreadsheet className="w-4 h-4 text-amber-400" />
            <span className="text-xs font-mono font-semibold uppercase tracking-wider text-slate-200">
              Personnel Custody Ledger
            </span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-slate-800 text-slate-300">
              {totalElements} entries
            </span>
          </div>

          <button
            onClick={fetchAssignments}
            disabled={tableLoading}
            title="Refresh Ledger"
            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${tableLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>

        {/* Error State */}
        {fetchError && (
          <div className="p-8 text-center bg-rose-950/20 border-b border-rose-900/30">
            <AlertCircle className="w-8 h-8 text-rose-500 mx-auto mb-2" />
            <p className="text-sm font-semibold text-rose-300 mb-1">{fetchError}</p>
            <p className="text-xs text-slate-400 font-mono mb-4">
              Unable to complete query on /api/assignments.
            </p>
            <button
              onClick={fetchAssignments}
              className="px-4 py-1.5 rounded bg-rose-900/60 hover:bg-rose-900 text-rose-200 text-xs font-mono transition-colors"
            >
              Retry Query
            </button>
          </div>
        )}

        {/* Loading Skeleton */}
        {tableLoading && (
          <div className="p-6 space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div
                key={i}
                className="h-10 rounded-lg bg-slate-900/80 animate-pulse border border-slate-800/50"
              />
            ))}
          </div>
        )}

        {/* Empty State */}
        {!tableLoading && !fetchError && assignments.length === 0 && (
          <div className="p-12 text-center">
            <Package className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <h3 className="text-sm font-semibold text-slate-300">No assignments recorded yet</h3>
            <p className="text-xs text-slate-500 font-mono mt-1 max-w-sm mx-auto">
              There are no custody records matching the current filter parameters in the audit ledger.
            </p>
            <button
              onClick={() => setShowAddModal(true)}
              className="mt-4 inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-amber-950/60 border border-amber-500/40 text-amber-400 hover:bg-amber-900/50 text-xs font-mono transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Issue First Assignment</span>
            </button>
          </div>
        )}

        {/* Desktop / Tablet Table View (hidden on small mobile screens) */}
        {!tableLoading && !fetchError && assignments.length > 0 && (
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-900 font-mono uppercase text-[11px] text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="p-3.5">Personnel</th>
                  <th className="p-3.5">Equipment</th>
                  <th className="p-3.5">Base</th>
                  <th className="p-3.5 text-right">Quantity</th>
                  <th className="p-3.5">Status</th>
                  <th className="p-3.5">Assigned Date</th>
                  <th className="p-3.5">Expended/Returned Date</th>
                  <th className="p-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
                {assignments.map((a) => {
                  const resolvedDate = a.status === 'expended'
                    ? (a.expendedDate || a.expended_date || '—')
                    : a.status === 'returned'
                    ? (a.returnedDate || a.returned_date || '—')
                    : '—';

                  return (
                    <tr key={a.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="p-3.5 font-medium text-white whitespace-nowrap">
                        <div>{a.personnelName || a.personnel_name}</div>
                        <div className="text-[10px] text-slate-400 font-normal">
                          {a.personnelIdNo || a.personnel_id_no || 'ID: Standard'}
                        </div>
                      </td>
                      <td className="p-3.5 text-slate-200">
                        {a.equipmentName || a.equipment_name || `Equipment #${a.equipmentTypeId}`}
                      </td>
                      <td className="p-3.5 text-slate-300 whitespace-nowrap">
                        {a.baseName || a.base_name || `Base #${a.baseId}`}
                      </td>
                      <td className="p-3.5 text-right font-bold text-amber-400">
                        {Number(a.quantity).toLocaleString()}
                      </td>
                      <td className="p-3.5 whitespace-nowrap">
                        <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-mono border uppercase tracking-wider ${getStatusBadge(a.status)}`}>
                          {a.status}
                        </span>
                      </td>
                      <td className="p-3.5 text-slate-300 whitespace-nowrap">
                        {a.assignedDate || a.assigned_date}
                      </td>
                      <td className="p-3.5 text-slate-300 whitespace-nowrap">
                        {resolvedDate}
                      </td>
                      <td className="p-3.5 text-right whitespace-nowrap">
                        {a.status === 'assigned' ? (
                          <div className="inline-flex items-center space-x-1.5">
                            <button
                              onClick={() => openActionModal(a, 'return')}
                              className="inline-flex items-center px-2 py-1 rounded bg-emerald-950 hover:bg-emerald-900 text-emerald-300 border border-emerald-500/40 text-[10px] font-mono transition-colors"
                              title="Mark as Returned to Armory"
                            >
                              <RotateCcw className="w-3 h-3 mr-1" />
                              Return
                            </button>
                            <button
                              onClick={() => openActionModal(a, 'expend')}
                              className="inline-flex items-center px-2 py-1 rounded bg-rose-950 hover:bg-rose-900 text-rose-300 border border-rose-500/40 text-[10px] font-mono transition-colors"
                              title="Mark as Expended in Field"
                            >
                              <Flame className="w-3 h-3 mr-1" />
                              Expend
                            </button>
                          </div>
                        ) : (
                          <span className="text-[10px] text-slate-500 font-mono">
                            {a.status === 'expended' ? 'Expended' : 'Returned'}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Mobile Stacked Cards View (visible on small screens only) */}
        {!tableLoading && !fetchError && assignments.length > 0 && (
          <div className="md:hidden divide-y divide-slate-800/70">
            {assignments.map((a) => {
              const resolvedDate = a.status === 'expended'
                ? (a.expendedDate || a.expended_date || '—')
                : a.status === 'returned'
                ? (a.returnedDate || a.returned_date || '—')
                : '—';

              return (
                <div key={a.id} className="p-4 space-y-2.5 font-mono text-xs">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-semibold text-white block">
                        {a.personnelName || a.personnel_name}
                      </span>
                      <span className="text-[10px] text-slate-400">
                        {a.personnelIdNo || a.personnel_id_no || 'Standard Personnel'}
                      </span>
                    </div>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-mono border uppercase ${getStatusBadge(a.status)}`}>
                      {a.status}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-slate-400 text-[11px]">
                    <div>
                      <span className="text-slate-500 block">Equipment</span>
                      <span className="text-slate-200 font-medium">
                        {a.equipmentName || a.equipment_name}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Quantity</span>
                      <span className="text-amber-400 font-bold">
                        {Number(a.quantity).toLocaleString()} units
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Base</span>
                      <span className="text-slate-200">{a.baseName || a.base_name}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Assigned Date</span>
                      <span className="text-slate-200">{a.assignedDate || a.assigned_date}</span>
                    </div>
                    {a.status !== 'assigned' && (
                      <div className="col-span-2">
                        <span className="text-slate-500 block">
                          {a.status === 'expended' ? 'Expended Date' : 'Returned Date'}
                        </span>
                        <span className="text-slate-200">{resolvedDate}</span>
                      </div>
                    )}
                  </div>

                  {a.notes && (
                    <p className="text-[11px] text-slate-400 bg-slate-900/60 p-2 rounded border border-slate-800/60 italic">
                      "{a.notes}"
                    </p>
                  )}

                  <div className="pt-2 border-t border-slate-900 flex items-center justify-between text-[11px]">
                    <span className="text-slate-500">Ref #ASN-{a.id}</span>

                    {a.status === 'assigned' && (
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => openActionModal(a, 'return')}
                          className="px-2 py-1 rounded bg-emerald-950 text-emerald-300 border border-emerald-500/40 text-[10px]"
                        >
                          Return
                        </button>
                        <button
                          onClick={() => openActionModal(a, 'expend')}
                          className="px-2 py-1 rounded bg-rose-950 text-rose-300 border border-rose-500/40 text-[10px]"
                        >
                          Expend
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Pagination Controls */}
        {!tableLoading && !fetchError && totalPages > 0 && (
          <div className="p-3.5 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs font-mono text-slate-400 bg-slate-950/30">
            <div>
              Showing page <span className="text-slate-200 font-semibold">{page + 1}</span> of{' '}
              <span className="text-slate-200 font-semibold">{Math.max(1, totalPages)}</span> ({totalElements} total records)
            </div>

            <div className="flex items-center space-x-2">
              <button
                id="pagination-prev"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="inline-flex items-center px-2.5 py-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
              >
                <ChevronLeft className="w-3.5 h-3.5 mr-1" />
                Previous
              </button>

              <button
                id="pagination-next"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="inline-flex items-center px-2.5 py-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
              >
                Next
                <ChevronRight className="w-3.5 h-3.5 ml-1" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Assign Asset Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-lg bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            {/* Modal Title */}
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 rounded-lg bg-amber-950/60 border border-amber-500/30 text-amber-400">
                  <UserCheck className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-white tracking-tight">Issue Asset to Personnel</h2>
                  <p className="text-[11px] font-mono text-slate-400">
                    MAMS CUSTODY & FIELD EXPENDITURE DIRECTIVE
                  </p>
                </div>
              </div>
              <button
                onClick={() => {
                  setShowAddModal(false);
                  resetForm();
                }}
                className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* General Form Error Alert */}
            {formGeneralError && (
              <div className="p-3 rounded-lg bg-rose-950/40 border border-rose-500/40 text-rose-300 text-xs font-mono flex items-start space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                <span>{formGeneralError}</span>
              </div>
            )}

            {/* Assignment Form */}
            <form onSubmit={handleSubmitAssignment} className="space-y-3.5 font-mono text-xs">
              {/* Base Armory */}
              <div>
                <label className="block text-slate-400 mb-1">
                  Base Armory <span className="text-rose-400">*</span>
                </label>
                {role === 'ADMIN' ? (
                  <select
                    id="form-base-select"
                    required
                    value={formBaseId}
                    onChange={(e) => {
                      setFormBaseId(e.target.value);
                      if (fieldErrors.baseId) setFieldErrors((prev) => ({ ...prev, baseId: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.baseId ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-amber-500/60 focus:outline-none`}
                  >
                    <option value="">Select Base</option>
                    {bases.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name} ({b.location})
                      </option>
                    ))}
                  </select>
                ) : (
                  <div
                    title="Locked to your command jurisdiction"
                    className="w-full px-3 py-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 flex items-center justify-between"
                  >
                    <div className="flex items-center space-x-2">
                      <Building2 className="w-4 h-4 text-slate-500" />
                      <span>{baseName || `Base #${baseId}`}</span>
                    </div>
                    <span className="inline-flex items-center text-[10px] text-amber-400 bg-amber-950/40 px-2 py-0.5 rounded border border-amber-500/30">
                      <Lock className="w-3 h-3 mr-1" /> BASE COMMAND JURISDICTION
                    </span>
                  </div>
                )}
                {fieldErrors.baseId && (
                  <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.baseId}</p>
                )}
              </div>

              {/* Equipment Type */}
              <div>
                <label className="block text-slate-400 mb-1">
                  Equipment / Asset Type <span className="text-rose-400">*</span>
                </label>
                <select
                  id="form-equipment-select"
                  required
                  value={formEquipmentTypeId}
                  onChange={(e) => {
                    setFormEquipmentTypeId(e.target.value);
                    if (fieldErrors.equipmentTypeId) setFieldErrors((prev) => ({ ...prev, equipmentTypeId: null }));
                  }}
                  className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                    fieldErrors.equipmentTypeId ? 'border-rose-500' : 'border-slate-800'
                  } text-slate-200 focus:border-amber-500/60 focus:outline-none`}
                >
                  <option value="">Select Equipment</option>
                  {equipmentTypes.map((eq) => (
                    <option key={eq.id} value={eq.id}>
                      {eq.name} • {eq.category?.toUpperCase()} ({eq.unit})
                    </option>
                  ))}
                </select>
                {fieldErrors.equipmentTypeId && (
                  <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.equipmentTypeId}</p>
                )}
              </div>

              {/* Personnel Name and Personnel ID */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    Personnel Name <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="text"
                    id="form-personnel-name-input"
                    required
                    placeholder="e.g. Sgt. David Miller"
                    value={formPersonnelName}
                    onChange={(e) => {
                      setFormPersonnelName(e.target.value);
                      if (fieldErrors.personnelName) setFieldErrors((prev) => ({ ...prev, personnelName: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.personnelName ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-amber-500/60 focus:outline-none`}
                  />
                  {fieldErrors.personnelName && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.personnelName}</p>
                  )}
                </div>

                <div>
                  <label className="block text-slate-400 mb-1">
                    Personnel ID No <span className="text-slate-500 text-[10px]">(optional)</span>
                  </label>
                  <input
                    type="text"
                    id="form-personnel-id-input"
                    placeholder="e.g. MIL-88492"
                    value={formPersonnelIdNo}
                    onChange={(e) => setFormPersonnelIdNo(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 focus:border-amber-500/60 focus:outline-none"
                  />
                </div>
              </div>

              {/* Quantity and Assigned Date */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    Quantity <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="number"
                    id="form-quantity-input"
                    min="1"
                    step="1"
                    required
                    placeholder="e.g. 1"
                    value={formQuantity}
                    onChange={(e) => {
                      setFormQuantity(e.target.value);
                      if (fieldErrors.quantity) setFieldErrors((prev) => ({ ...prev, quantity: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.quantity ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-amber-500/60 focus:outline-none`}
                  />
                  {fieldErrors.quantity && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.quantity}</p>
                  )}
                </div>

                <div>
                  <label className="block text-slate-400 mb-1">
                    Assigned Date <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="date"
                    id="form-assigned-date-input"
                    max={todayStr}
                    required
                    value={formAssignedDate}
                    onChange={(e) => {
                      setFormAssignedDate(e.target.value);
                      if (fieldErrors.assignedDate) setFieldErrors((prev) => ({ ...prev, assignedDate: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.assignedDate ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-amber-500/60 focus:outline-none`}
                  />
                  {fieldErrors.assignedDate && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.assignedDate}</p>
                  )}
                </div>
              </div>

              {/* Notes */}
              <div>
                <label className="block text-slate-400 mb-1">
                  Assignment Directive / Mission Purpose <span className="text-slate-500 text-[10px]">(optional)</span>
                </label>
                <textarea
                  id="form-notes-input"
                  rows="2"
                  placeholder="Operational purpose, firing range exercise, deployment orders..."
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 focus:border-amber-500/60 focus:outline-none"
                />
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-end space-x-2.5 pt-4 border-t border-slate-800/80">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddModal(false);
                    resetForm();
                  }}
                  className="px-4 py-2 rounded-lg bg-slate-800/80 hover:bg-slate-800 text-slate-300 font-mono text-xs transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="submit-create-assignment-btn"
                  disabled={submitting}
                  className="inline-flex items-center space-x-1.5 px-4 py-2 rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold font-mono text-xs uppercase tracking-wider transition-all disabled:opacity-50"
                >
                  {submitting && <RefreshCw className="w-3.5 h-3.5 animate-spin mr-1" />}
                  <span>{submitting ? 'Issuing...' : 'Authorize Custody'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Action Dialog Modal (Expend / Return) */}
      {actionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-md bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
              <div className="flex items-center space-x-2.5">
                <div className={`p-2 rounded-lg border ${
                  actionModal.type === 'expend'
                    ? 'bg-rose-950/60 border-rose-500/30 text-rose-400'
                    : 'bg-emerald-950/60 border-emerald-500/30 text-emerald-400'
                }`}>
                  {actionModal.type === 'expend' ? <Flame className="w-4 h-4" /> : <RotateCcw className="w-4 h-4" />}
                </div>
                <div>
                  <h3 className="text-base font-bold text-white tracking-tight">
                    {actionModal.type === 'expend' ? 'Mark Asset as Expended' : 'Mark Asset as Returned'}
                  </h3>
                  <p className="text-[11px] font-mono text-slate-400">
                    REF #ASN-{actionModal.assignment.id} • {actionModal.assignment.personnelName}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setActionModal(null)}
                className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmitAction} className="space-y-3.5 font-mono text-xs">
              <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 space-y-1">
                <div className="text-slate-400 flex justify-between">
                  <span>Equipment:</span>
                  <span className="text-white font-medium">{actionModal.assignment.equipmentName}</span>
                </div>
                <div className="text-slate-400 flex justify-between">
                  <span>Quantity:</span>
                  <span className="text-amber-400 font-bold">{actionModal.assignment.quantity} units</span>
                </div>
                <div className="text-slate-400 flex justify-between">
                  <span>Assigned Date:</span>
                  <span className="text-slate-300">{actionModal.assignment.assignedDate}</span>
                </div>
              </div>

              <div>
                <label className="block text-slate-400 mb-1">
                  {actionModal.type === 'expend' ? 'Expended Date' : 'Returned Date'} <span className="text-rose-400">*</span>
                </label>
                <input
                  type="date"
                  max={todayStr}
                  required
                  value={actionDate}
                  onChange={(e) => setActionDate(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-amber-500/60"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1">
                  Logistics Notes <span className="text-slate-500 text-[10px]">(optional)</span>
                </label>
                <textarea
                  rows="2"
                  placeholder={actionModal.type === 'expend'
                    ? "Combat usage, firing drill rounds spent, decommissioning detail..."
                    : "Condition inspected, clean return, armory rack restocked..."}
                  value={actionNotes}
                  onChange={(e) => setActionNotes(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-amber-500/60"
                />
              </div>

              <div className="flex items-center justify-end space-x-2 pt-3 border-t border-slate-800/80">
                <button
                  type="button"
                  onClick={() => setActionModal(null)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionSubmitting}
                  className={`inline-flex items-center space-x-1 px-4 py-2 rounded-lg font-bold uppercase tracking-wider text-slate-950 transition-all ${
                    actionModal.type === 'expend'
                      ? 'bg-rose-500 hover:bg-rose-400'
                      : 'bg-emerald-500 hover:bg-emerald-400'
                  }`}
                >
                  {actionSubmitting && <RefreshCw className="w-3 h-3 animate-spin mr-1" />}
                  <span>{actionModal.type === 'expend' ? 'Confirm Expended' : 'Confirm Returned'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AssignmentsPage;
