import React, { useState, useEffect, useMemo } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import {
  ArrowLeftRight,
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
  Truck,
  User as UserIcon,
  X,
  FileSpreadsheet,
  ArrowRight,
  ArrowDownLeft,
  ArrowUpRight,
  RotateCcw,
  Check,
  Send
} from 'lucide-react';

export const TransfersPage = () => {
  const { user, role, baseId, baseName } = useAuth();

  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);

  // Lookups
  const [bases, setBases] = useState([]);
  const [equipmentTypes, setEquipmentTypes] = useState([]);
  const [lookupsLoading, setLookupsLoading] = useState(true);

  // Table Data & State
  const [transfers, setTransfers] = useState([]);
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
  const [filterDirection, setFilterDirection] = useState(''); // '' (All), 'in', 'out'

  // Modal / Form State
  const [showFormModal, setShowFormModal] = useState(false);
  const [formEquipmentTypeId, setFormEquipmentTypeId] = useState('');
  const [formFromBaseId, setFormFromBaseId] = useState(baseId ? String(baseId) : '');
  const [formToBaseId, setFormToBaseId] = useState('');
  const [formQuantity, setFormQuantity] = useState('');
  const [formTransferDate, setFormTransferDate] = useState(todayStr);
  const [formStatus, setFormStatus] = useState('completed');
  const [formNotes, setFormNotes] = useState('');

  // Form Submission & Status Update State
  const [submitting, setSubmitting] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formGeneralError, setFormGeneralError] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);

  // Load lookup metadata (bases & equipment types)
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

  // Fetch paginated transfers
  const fetchTransfers = async () => {
    setTableLoading(true);
    setFetchError(null);

    try {
      const params = {
        page,
        size: pageSize,
        sort: 'transferDate,desc',
      };

      if (filterStartDate) params.start_date = filterStartDate;
      if (filterEndDate) params.end_date = filterEndDate;

      if (role === 'ADMIN') {
        if (filterBaseId) params.base_id = filterBaseId;
      } else if (baseId) {
        params.base_id = baseId;
      }

      if (filterEquipmentTypeId) params.equipment_type_id = filterEquipmentTypeId;
      if (filterDirection) params.direction = filterDirection;

      const res = await api.get('/api/transfers', { params });
      const payload = res.data?.data || res.data;

      setTransfers(payload.content || []);
      setTotalPages(payload.totalPages || 0);
      setTotalElements(payload.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch transfers:', err);
      setFetchError(
        err.response?.data?.error || 'Unable to retrieve transfer manifests. Please verify your connection or try again.'
      );
    } finally {
      setTableLoading(false);
    }
  };

  useEffect(() => {
    fetchTransfers();
  }, [page, filterStartDate, filterEndDate, filterBaseId, filterEquipmentTypeId, filterDirection]);

  // Reset form inputs
  const resetForm = () => {
    setFormEquipmentTypeId('');
    setFormFromBaseId(baseId ? String(baseId) : '');
    setFormToBaseId('');
    setFormQuantity('');
    setFormTransferDate(todayStr);
    setFormStatus('completed');
    setFormNotes('');
    setFieldErrors({});
    setFormGeneralError(null);
  };

  // Handle Create Transfer
  const handleSubmitTransfer = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFieldErrors({});
    setFormGeneralError(null);

    const errors = {};
    if (!formEquipmentTypeId) {
      errors.equipmentTypeId = 'Please select equipment type';
    }
    if (!formFromBaseId) {
      errors.fromBaseId = 'Please select origin base';
    }
    if (!formToBaseId) {
      errors.toBaseId = 'Please select destination base';
    }
    if (formFromBaseId && formToBaseId && String(formFromBaseId) === String(formToBaseId)) {
      errors.toBaseId = 'Source and destination bases cannot be identical';
    }

    // Role check for non-admins
    if (role !== 'ADMIN' && baseId) {
      const ownBaseStr = String(baseId);
      if (String(formFromBaseId) !== ownBaseStr && String(formToBaseId) !== ownBaseStr) {
        errors.fromBaseId = 'Transfers must involve your assigned base as either sender or receiver';
      }
    }

    const qtyNum = parseInt(formQuantity, 10);
    if (!qtyNum || qtyNum <= 0) {
      errors.quantity = 'Quantity must be at least 1';
    }
    if (!formTransferDate) {
      errors.transferDate = 'Transfer date is required';
    } else if (formTransferDate > todayStr) {
      errors.transferDate = 'Transfer date cannot be in the future';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setSubmitting(false);
      return;
    }

    const payload = {
      equipment_type_id: Number(formEquipmentTypeId),
      from_base_id: Number(formFromBaseId),
      to_base_id: Number(formToBaseId),
      quantity: qtyNum,
      transfer_date: formTransferDate,
      status: formStatus,
      notes: formNotes.trim() || null,
    };

    try {
      const res = await api.post('/api/transfers', payload);
      const createdItem = res.data?.data || res.data;

      setToastMessage({
        type: 'success',
        text: `Transfer manifest #TRF-${createdItem.id} initiated for ${createdItem.quantity}x ${createdItem.equipmentName || 'units'}.`,
      });

      resetForm();
      setShowFormModal(false);

      setTimeout(() => setToastMessage(null), 5000);

      if (page !== 0) {
        setPage(0);
      } else {
        fetchTransfers();
      }
    } catch (err) {
      console.error('Error creating transfer:', err);
      const errData = err.response?.data;

      if (errData?.details && typeof errData.details === 'object') {
        const mappedErrors = {};
        for (const [key, msg] of Object.entries(errData.details)) {
          if (key === 'equipmentTypeId' || key === 'equipment_type_id') mappedErrors.equipmentTypeId = msg;
          else if (key === 'fromBaseId' || key === 'from_base_id') mappedErrors.fromBaseId = msg;
          else if (key === 'toBaseId' || key === 'to_base_id') mappedErrors.toBaseId = msg;
          else if (key === 'quantity') mappedErrors.quantity = msg;
          else if (key === 'transferDate' || key === 'transfer_date') mappedErrors.transferDate = msg;
          else mappedErrors[key] = msg;
        }
        setFieldErrors(mappedErrors);
      }

      setFormGeneralError(
        errData?.error || 'Failed to dispatch transfer. Please verify the input values.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  // Handle Status Update
  const handleUpdateStatus = async (transferId, nextStatus) => {
    setStatusUpdatingId(transferId);
    try {
      await api.patch(`/api/transfers/${transferId}/status`, { status: nextStatus });

      setToastMessage({
        type: 'success',
        text: `Transfer manifest #TRF-${transferId} status updated to '${nextStatus}'.`,
      });
      setTimeout(() => setToastMessage(null), 5000);

      fetchTransfers();
    } catch (err) {
      console.error('Failed to update transfer status:', err);
      setToastMessage({
        type: 'error',
        text: err.response?.data?.error || `Failed to update status for transfer #TRF-${transferId}.`,
      });
      setTimeout(() => setToastMessage(null), 5000);
    } finally {
      setStatusUpdatingId(null);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'completed':
        return 'bg-emerald-950/60 text-emerald-400 border-emerald-500/40';
      case 'in_transit':
        return 'bg-cyan-950/60 text-cyan-400 border-cyan-500/40 animate-pulse';
      case 'pending':
        return 'bg-amber-950/60 text-amber-400 border-amber-500/40';
      case 'cancelled':
        return 'bg-rose-950/60 text-rose-400 border-rose-500/40';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  const clearFilters = () => {
    setFilterStartDate('');
    setFilterEndDate('');
    if (role === 'ADMIN') setFilterBaseId('');
    setFilterEquipmentTypeId('');
    setFilterDirection('');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* Top Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-1.5 rounded-lg bg-cyan-950/60 border border-cyan-500/30 text-cyan-400">
              <ArrowLeftRight className="w-5 h-5" />
            </span>
            <h1 className="text-xl md:text-2xl font-bold tracking-tight text-white">
              Inter-Base Asset Transfers
            </h1>
          </div>
          <p className="text-xs font-mono text-slate-400 mt-1">
            LOGISTICS RELOCATION & TRANSIT MANIFEST CONTROL • STATUS LIFECYCLE
          </p>
        </div>

        {/* Action Button */}
        <div className="flex items-center space-x-3">
          <button
            id="open-create-transfer-modal"
            onClick={() => {
              setFieldErrors({});
              setFormGeneralError(null);
              setShowFormModal(true);
            }}
            className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs uppercase font-mono tracking-wider transition-all shadow-lg shadow-cyan-500/20 active:scale-95"
          >
            <Plus className="w-4 h-4 stroke-[3]" />
            <span>Create Transfer</span>
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
            <Filter className="w-3.5 h-3.5 mr-1.5 text-cyan-400" />
            FILTER TRANSFERS
          </span>

          <div className="flex items-center space-x-3">
            {/* Direction Toggle Control */}
            <div className="inline-flex rounded-lg bg-slate-950 p-0.5 border border-slate-800 text-[11px] font-mono">
              <button
                type="button"
                onClick={() => {
                  setFilterDirection('');
                  setPage(0);
                }}
                className={`px-2.5 py-1 rounded-md transition-colors ${
                  filterDirection === ''
                    ? 'bg-cyan-500/20 text-cyan-400 font-bold border border-cyan-500/40'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                All Movements
              </button>
              <button
                type="button"
                onClick={() => {
                  setFilterDirection('in');
                  setPage(0);
                }}
                className={`inline-flex items-center px-2.5 py-1 rounded-md transition-colors ${
                  filterDirection === 'in'
                    ? 'bg-emerald-500/20 text-emerald-400 font-bold border border-emerald-500/40'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <ArrowDownLeft className="w-3 h-3 mr-1" />
                Inbound
              </button>
              <button
                type="button"
                onClick={() => {
                  setFilterDirection('out');
                  setPage(0);
                }}
                className={`inline-flex items-center px-2.5 py-1 rounded-md transition-colors ${
                  filterDirection === 'out'
                    ? 'bg-amber-500/20 text-amber-400 font-bold border border-amber-500/40'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <ArrowUpRight className="w-3 h-3 mr-1" />
                Outbound
              </button>
            </div>

            {(filterStartDate || filterEndDate || (role === 'ADMIN' && filterBaseId) || filterEquipmentTypeId || filterDirection) && (
              <button
                onClick={clearFilters}
                className="text-[11px] text-slate-400 hover:text-cyan-400 underline decoration-dotted"
              >
                Reset
              </button>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
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
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-cyan-500/60 focus:outline-none"
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
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-cyan-500/60 focus:outline-none"
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
                className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-cyan-500/60 focus:outline-none"
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
                title="Your access is scoped to transfers involving your assigned base"
                className="w-full px-3 py-2 rounded-lg bg-slate-900/80 border border-slate-800 text-xs text-slate-400 font-mono flex items-center justify-between"
              >
                <span className="truncate">{baseName || 'Assigned Base'}</span>
                <span className="text-[10px] text-cyan-400 bg-cyan-950/40 px-1.5 py-0.5 rounded border border-cyan-500/30">
                  SCOPED
                </span>
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
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-cyan-500/60 focus:outline-none"
            >
              <option value="">All Equipment</option>
              {equipmentTypes.map((eq) => (
                <option key={eq.id} value={eq.id}>
                  {eq.name} ({eq.category})
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* History Table Section */}
      <div className="rounded-xl border border-slate-800 overflow-hidden bg-[#0c1322] shadow-xl">
        <div className="p-4 border-b border-slate-800/80 flex items-center justify-between bg-slate-950/40">
          <div className="flex items-center space-x-2">
            <FileSpreadsheet className="w-4 h-4 text-cyan-400" />
            <span className="text-xs font-mono font-semibold uppercase tracking-wider text-slate-200">
              Transfer Manifests Ledger
            </span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-slate-800 text-slate-300">
              {totalElements} entries
            </span>
          </div>

          <button
            onClick={fetchTransfers}
            disabled={tableLoading}
            title="Refresh Manifests"
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
              Unable to complete query on /api/transfers.
            </p>
            <button
              onClick={fetchTransfers}
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
        {!tableLoading && !fetchError && transfers.length === 0 && (
          <div className="p-12 text-center">
            <Truck className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <h3 className="text-sm font-semibold text-slate-300">No transfers recorded yet</h3>
            <p className="text-xs text-slate-500 font-mono mt-1 max-w-sm mx-auto">
              There are no transfer manifests matching the current filter parameters in the audit ledger.
            </p>
            <button
              onClick={() => setShowFormModal(true)}
              className="mt-4 inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-cyan-950/60 border border-cyan-500/40 text-cyan-400 hover:bg-cyan-900/50 text-xs font-mono transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Initiate First Transfer</span>
            </button>
          </div>
        )}

        {/* Desktop / Tablet Table View (hidden on mobile) */}
        {!tableLoading && !fetchError && transfers.length > 0 && (
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-900 font-mono uppercase text-[11px] text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="p-3.5">Date</th>
                  <th className="p-3.5">Equipment</th>
                  <th className="p-3.5">From Base</th>
                  <th className="p-3.5">To Base</th>
                  <th className="p-3.5 text-right">Quantity</th>
                  <th className="p-3.5">Status</th>
                  <th className="p-3.5">Recorded By</th>
                  <th className="p-3.5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
                {transfers.map((t) => {
                  const isUpdating = statusUpdatingId === t.id;

                  return (
                    <tr key={t.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="p-3.5 text-slate-200 whitespace-nowrap">
                        {t.transferDate || t.transfer_date}
                      </td>
                      <td className="p-3.5 text-white font-medium">
                        {t.equipmentName || t.equipment_name || `Equipment #${t.equipmentTypeId || t.equipment_type_id}`}
                      </td>
                      <td className="p-3.5 text-slate-300 font-medium">
                        {t.fromBaseName || t.from_base_name || `Base #${t.fromBaseId || t.from_base_id}`}
                      </td>
                      <td className="p-3.5 text-slate-300 font-medium">
                        {t.toBaseName || t.to_base_name || `Base #${t.toBaseId || t.to_base_id}`}
                      </td>
                      <td className="p-3.5 text-right font-bold text-cyan-400">
                        {Number(t.quantity).toLocaleString()}
                      </td>
                      <td className="p-3.5 whitespace-nowrap">
                        <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-mono border uppercase tracking-wider ${getStatusBadge(t.status)}`}>
                          {t.status}
                        </span>
                      </td>
                      <td className="p-3.5 text-slate-400 whitespace-nowrap">
                        <span className="inline-flex items-center text-[11px] bg-slate-900 border border-slate-800 px-2 py-0.5 rounded">
                          <UserIcon className="w-2.5 h-2.5 mr-1 text-slate-500" />
                          @{t.createdByUsername || t.created_by_username || 'system'}
                        </span>
                      </td>
                      <td className="p-3.5 text-right whitespace-nowrap">
                        {/* Status Transition Action Buttons */}
                        {t.status === 'pending' && (
                          <div className="inline-flex items-center space-x-1">
                            <button
                              disabled={isUpdating}
                              onClick={() => handleUpdateStatus(t.id, 'in_transit')}
                              className="px-2 py-1 rounded bg-cyan-950 hover:bg-cyan-900 text-cyan-300 border border-cyan-500/40 text-[10px] font-mono transition-colors disabled:opacity-50"
                              title="Mark as In Transit"
                            >
                              Dispatch
                            </button>
                            <button
                              disabled={isUpdating}
                              onClick={() => handleUpdateStatus(t.id, 'completed')}
                              className="px-2 py-1 rounded bg-emerald-950 hover:bg-emerald-900 text-emerald-300 border border-emerald-500/40 text-[10px] font-mono transition-colors disabled:opacity-50"
                              title="Mark as Completed"
                            >
                              Complete
                            </button>
                            <button
                              disabled={isUpdating}
                              onClick={() => handleUpdateStatus(t.id, 'cancelled')}
                              className="px-2 py-1 rounded bg-rose-950 hover:bg-rose-900 text-rose-300 border border-rose-500/40 text-[10px] font-mono transition-colors disabled:opacity-50"
                              title="Cancel Transfer"
                            >
                              Cancel
                            </button>
                          </div>
                        )}

                        {t.status === 'in_transit' && (
                          <div className="inline-flex items-center space-x-1">
                            <button
                              disabled={isUpdating}
                              onClick={() => handleUpdateStatus(t.id, 'completed')}
                              className="px-2.5 py-1 rounded bg-emerald-950 hover:bg-emerald-900 text-emerald-300 border border-emerald-500/40 text-[10px] font-mono transition-colors disabled:opacity-50 inline-flex items-center"
                              title="Receive into Destination Base"
                            >
                              <Check className="w-3 h-3 mr-1" />
                              Receive
                            </button>
                            <button
                              disabled={isUpdating}
                              onClick={() => handleUpdateStatus(t.id, 'cancelled')}
                              className="px-2 py-1 rounded bg-rose-950 hover:bg-rose-900 text-rose-300 border border-rose-500/40 text-[10px] font-mono transition-colors disabled:opacity-50"
                              title="Cancel Transfer"
                            >
                              Cancel
                            </button>
                          </div>
                        )}

                        {t.status === 'completed' && (
                          <span className="text-[10px] text-slate-500 font-mono">Archived</span>
                        )}

                        {t.status === 'cancelled' && (
                          <span className="text-[10px] text-rose-500/70 font-mono">Terminated</span>
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
        {!tableLoading && !fetchError && transfers.length > 0 && (
          <div className="md:hidden divide-y divide-slate-800/70">
            {transfers.map((t) => {
              const isUpdating = statusUpdatingId === t.id;

              return (
                <div key={t.id} className="p-4 space-y-2.5 font-mono text-xs">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-white">
                      {t.equipmentName || t.equipment_name || 'Equipment'}
                    </span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-mono border uppercase ${getStatusBadge(t.status)}`}>
                      {t.status}
                    </span>
                  </div>

                  <div className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800 flex items-center justify-between text-[11px]">
                    <div className="text-left">
                      <span className="text-slate-500 block text-[9px] uppercase">Origin</span>
                      <span className="text-slate-200 font-medium">
                        {t.fromBaseName || t.from_base_name || `Base #${t.fromBaseId}`}
                      </span>
                    </div>
                    <ArrowRight className="w-4 h-4 text-cyan-400 mx-2 shrink-0" />
                    <div className="text-right">
                      <span className="text-slate-500 block text-[9px] uppercase">Destination</span>
                      <span className="text-slate-200 font-medium">
                        {t.toBaseName || t.to_base_name || `Base #${t.toBaseId}`}
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-slate-400 text-[11px]">
                    <div>
                      <span className="text-slate-500 block">Date</span>
                      <span className="text-slate-200">{t.transferDate || t.transfer_date}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Quantity</span>
                      <span className="text-cyan-400 font-bold">
                        {Number(t.quantity).toLocaleString()} units
                      </span>
                    </div>
                  </div>

                  {t.notes && (
                    <p className="text-[11px] text-slate-400 bg-slate-900/60 p-2 rounded border border-slate-800/60 italic">
                      "{t.notes}"
                    </p>
                  )}

                  <div className="pt-2 border-t border-slate-900 flex items-center justify-between text-[11px] text-slate-400">
                    <span className="inline-flex items-center bg-slate-900 px-1.5 py-0.5 rounded border border-slate-800">
                      <UserIcon className="w-2.5 h-2.5 mr-1 text-slate-500" />
                      @{t.createdByUsername || t.created_by_username || 'system'}
                    </span>

                    {/* Mobile Action Buttons */}
                    <div className="flex items-center space-x-1.5">
                      {t.status === 'pending' && (
                        <>
                          <button
                            disabled={isUpdating}
                            onClick={() => handleUpdateStatus(t.id, 'in_transit')}
                            className="px-2 py-0.5 rounded bg-cyan-950 text-cyan-300 border border-cyan-500/40 text-[10px]"
                          >
                            Dispatch
                          </button>
                          <button
                            disabled={isUpdating}
                            onClick={() => handleUpdateStatus(t.id, 'completed')}
                            className="px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-500/40 text-[10px]"
                          >
                            Complete
                          </button>
                        </>
                      )}
                      {t.status === 'in_transit' && (
                        <button
                          disabled={isUpdating}
                          onClick={() => handleUpdateStatus(t.id, 'completed')}
                          className="px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-500/40 text-[10px]"
                        >
                          Receive
                        </button>
                      )}
                    </div>
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

      {/* Create Transfer Modal */}
      {showFormModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-lg bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            {/* Modal Title */}
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 rounded-lg bg-cyan-950/60 border border-cyan-500/30 text-cyan-400">
                  <Truck className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-white tracking-tight">Initiate Base-to-Base Transfer</h2>
                  <p className="text-[11px] font-mono text-slate-400">
                    LOGISTICS RELOCATION MANIFEST DISPATCH
                  </p>
                </div>
              </div>
              <button
                onClick={() => {
                  setShowFormModal(false);
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

            {/* Transfer Form */}
            <form onSubmit={handleSubmitTransfer} className="space-y-3.5 font-mono text-xs">
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
                  } text-slate-200 focus:border-cyan-500/60 focus:outline-none`}
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

              {/* From Base and To Base */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    From Base (Origin) <span className="text-rose-400">*</span>
                  </label>
                  <select
                    id="form-from-base-select"
                    required
                    value={formFromBaseId}
                    onChange={(e) => {
                      setFormFromBaseId(e.target.value);
                      if (fieldErrors.fromBaseId) setFieldErrors((prev) => ({ ...prev, fromBaseId: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.fromBaseId ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-cyan-500/60 focus:outline-none`}
                  >
                    <option value="">Select Origin Base</option>
                    {bases.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name} {role !== 'ADMIN' && String(b.id) === String(baseId) ? '(Your Base)' : ''}
                      </option>
                    ))}
                  </select>
                  {fieldErrors.fromBaseId && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.fromBaseId}</p>
                  )}
                </div>

                <div>
                  <label className="block text-slate-400 mb-1">
                    To Base (Destination) <span className="text-rose-400">*</span>
                  </label>
                  <select
                    id="form-to-base-select"
                    required
                    value={formToBaseId}
                    onChange={(e) => {
                      setFormToBaseId(e.target.value);
                      if (fieldErrors.toBaseId) setFieldErrors((prev) => ({ ...prev, toBaseId: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.toBaseId ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-cyan-500/60 focus:outline-none`}
                  >
                    <option value="">Select Destination Base</option>
                    {bases.map((b) => (
                      <option key={b.id} value={b.id} disabled={String(b.id) === String(formFromBaseId)}>
                        {b.name} {role !== 'ADMIN' && String(b.id) === String(baseId) ? '(Your Base)' : ''}
                      </option>
                    ))}
                  </select>
                  {fieldErrors.toBaseId && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.toBaseId}</p>
                  )}
                </div>
              </div>

              {/* Quantity and Initial Status */}
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
                    placeholder="e.g. 10"
                    value={formQuantity}
                    onChange={(e) => {
                      setFormQuantity(e.target.value);
                      if (fieldErrors.quantity) setFieldErrors((prev) => ({ ...prev, quantity: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.quantity ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-cyan-500/60 focus:outline-none`}
                  />
                  {fieldErrors.quantity && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.quantity}</p>
                  )}
                </div>

                <div>
                  <label className="block text-slate-400 mb-1">Initial Status</label>
                  <select
                    id="form-status-select"
                    value={formStatus}
                    onChange={(e) => setFormStatus(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 focus:border-cyan-500/60 focus:outline-none"
                  >
                    <option value="completed">Completed (Immediate Transfer)</option>
                    <option value="in_transit">In Transit (Dispatched / En Route)</option>
                    <option value="pending">Pending (Awaiting Dispatch)</option>
                  </select>
                </div>
              </div>

              {/* Transfer Date */}
              <div>
                <label className="block text-slate-400 mb-1">
                  Transfer Date <span className="text-rose-400">*</span>
                </label>
                <input
                  type="date"
                  id="form-transfer-date-input"
                  max={todayStr}
                  required
                  value={formTransferDate}
                  onChange={(e) => {
                    setFormTransferDate(e.target.value);
                    if (fieldErrors.transferDate) setFieldErrors((prev) => ({ ...prev, transferDate: null }));
                  }}
                  className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                    fieldErrors.transferDate ? 'border-rose-500' : 'border-slate-800'
                  } text-slate-200 focus:border-cyan-500/60 focus:outline-none`}
                />
                {fieldErrors.transferDate && (
                  <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.transferDate}</p>
                )}
              </div>

              {/* Notes */}
              <div>
                <label className="block text-slate-400 mb-1">
                  Transit / Escort Notes <span className="text-slate-500 text-[10px]">(optional)</span>
                </label>
                <textarea
                  id="form-notes-input"
                  rows="2"
                  placeholder="Escort personnel, convoys manifest ID, transit route..."
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 focus:border-cyan-500/60 focus:outline-none"
                />
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-end space-x-2.5 pt-4 border-t border-slate-800/80">
                <button
                  type="button"
                  onClick={() => {
                    setShowFormModal(false);
                    resetForm();
                  }}
                  className="px-4 py-2 rounded-lg bg-slate-800/80 hover:bg-slate-800 text-slate-300 font-mono text-xs transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="submit-create-transfer-btn"
                  disabled={submitting}
                  className="inline-flex items-center space-x-1.5 px-4 py-2 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold font-mono text-xs uppercase tracking-wider transition-all disabled:opacity-50"
                >
                  {submitting && <RefreshCw className="w-3.5 h-3.5 animate-spin mr-1" />}
                  <span>{submitting ? 'Dispatching...' : 'Dispatch Transfer'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransfersPage;
