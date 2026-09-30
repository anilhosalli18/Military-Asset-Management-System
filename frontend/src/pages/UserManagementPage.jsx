import React, { useState, useEffect, useCallback } from 'react';
import { Navigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import {
  Users,
  UserPlus,
  ShieldAlert,
  CheckCircle2,
  AlertCircle,
  Building2,
  UserX,
  Edit3,
  KeyRound,
  Filter,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Shield,
  X,
  Eye,
  EyeOff,
  HelpCircle,
  Check,
} from 'lucide-react';

export const UserManagementPage = () => {
  const { user: currentUser, role: currentRole } = useAuth();

  // Strict role check: ADMIN only -- redirect non-admins
  if (currentRole !== 'ADMIN') {
    return <Navigate to="/dashboard" replace />;
  }

  // Lookups
  const [bases, setBases] = useState([]);

  // Table Data & State
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState(null);

  // Pagination State
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters State
  const [filterRole, setFilterRole] = useState('');
  const [filterBaseId, setFilterBaseId] = useState('');
  const [filterActive, setFilterActive] = useState(''); // '' | 'true' | 'false'

  // Create User Modal State
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createUsername, setCreateUsername] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [createPassword, setCreatePassword] = useState('');
  const [createShowPassword, setCreateShowPassword] = useState(false);
  const [createFullName, setCreateFullName] = useState('');
  const [createRole, setCreateRole] = useState('BASE_COMMANDER');
  const [createBaseId, setCreateBaseId] = useState('');
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState(null);

  // Edit User Modal State
  const [editUserModal, setEditUserModal] = useState(null); // target user object
  const [editFullName, setEditFullName] = useState('');
  const [editRole, setEditRole] = useState('BASE_COMMANDER');
  const [editBaseId, setEditBaseId] = useState('');
  const [editIsActive, setEditIsActive] = useState(true);
  const [editSubmitting, setEditSubmitting] = useState(false);
  const [editError, setEditError] = useState(null);

  // Reset Password Modal State
  const [resetPasswordModal, setResetPasswordModal] = useState(null); // target user object
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [resetSubmitting, setResetSubmitting] = useState(false);
  const [resetError, setResetError] = useState(null);

  // Deactivate Confirmation Modal State
  const [deactivateModal, setDeactivateModal] = useState(null); // target user object
  const [deactivateSubmitting, setDeactivateSubmitting] = useState(false);

  // Fetch Lookups (Bases)
  useEffect(() => {
    const fetchBases = async () => {
      try {
        const res = await api.get('/api/bases');
        const list = Array.isArray(res.data) ? res.data : (res.data?.data || []);
        setBases(list);
      } catch (err) {
        console.error('Failed to load military bases lookup:', err);
      }
    };
    fetchBases();
  }, []);

  // Fetch Users
  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: pageSize,
      };
      if (filterRole) params.role = filterRole;
      if (filterBaseId) params.base_id = filterBaseId;
      if (filterActive !== '') params.is_active = filterActive === 'true';

      const res = await api.get('/api/users', { params });
      const data = res.data?.data || res.data;

      if (data && Array.isArray(data.content)) {
        setUsers(data.content);
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
      } else if (Array.isArray(data)) {
        setUsers(data);
        setTotalPages(1);
        setTotalElements(data.length);
      } else {
        setUsers([]);
        setTotalPages(0);
        setTotalElements(0);
      }
    } catch (err) {
      console.error('Failed to fetch users:', err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to load user directory.';
      setFeedback({ type: 'error', message: msg });
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, filterRole, filterBaseId, filterActive]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  // Handle Create User
  const handleOpenCreateModal = () => {
    setCreateUsername('');
    setCreateEmail('');
    setCreatePassword('');
    setCreateShowPassword(false);
    setCreateFullName('');
    setCreateRole('BASE_COMMANDER');
    setCreateBaseId('');
    setCreateError(null);
    setShowCreateModal(true);
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setCreateError(null);

    if (createPassword.length < 8) {
      setCreateError('Password must be at least 8 characters in length.');
      return;
    }

    if (createRole !== 'ADMIN' && !createBaseId) {
      setCreateError('Non-admin personnel must be assigned to an active base.');
      return;
    }

    setCreateSubmitting(true);
    try {
      await api.post('/api/users', {
        username: createUsername.trim(),
        email: createEmail.trim(),
        password: createPassword,
        fullName: createFullName.trim(),
        role: createRole,
        baseId: createRole === 'ADMIN' ? null : Number(createBaseId),
      });

      setFeedback({
        type: 'success',
        message: `Account @${createUsername.trim()} has been provisioned successfully.`,
      });
      setShowCreateModal(false);
      setPage(0);
      fetchUsers();
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to provision user account.';
      setCreateError(msg);
    } finally {
      setCreateSubmitting(false);
    }
  };

  // Handle Edit User
  const handleOpenEditModal = (targetUser) => {
    setEditUserModal(targetUser);
    setEditFullName(targetUser.fullName || targetUser.full_name || '');
    setEditRole(targetUser.role);
    setEditBaseId(targetUser.baseId ? String(targetUser.baseId) : '');
    setEditIsActive(targetUser.isActive ?? targetUser.is_active ?? true);
    setEditError(null);
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!editUserModal) return;
    setEditError(null);

    const isOwnAccount = currentUser && String(currentUser.id) === String(editUserModal.id);

    if (isOwnAccount) {
      if (!editIsActive) {
        setEditError('Administrators cannot deactivate their own account.');
        return;
      }
      if (editRole !== 'ADMIN') {
        setEditError('Administrators cannot demote their own account.');
        return;
      }
    }

    if (editRole !== 'ADMIN' && !editBaseId) {
      setEditError('Non-admin personnel must be assigned to an active base.');
      return;
    }

    setEditSubmitting(true);
    try {
      await api.put(`/api/users/${editUserModal.id}`, {
        fullName: editFullName.trim(),
        role: editRole,
        baseId: editRole === 'ADMIN' ? null : Number(editBaseId),
        isActive: editIsActive,
      });

      setFeedback({
        type: 'success',
        message: `User @${editUserModal.username} updated successfully.`,
      });
      setEditUserModal(null);
      fetchUsers();
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to update user profile.';
      setEditError(msg);
    } finally {
      setEditSubmitting(false);
    }
  };

  // Handle Reset Password
  const handleOpenResetPasswordModal = (targetUser) => {
    setResetPasswordModal(targetUser);
    setNewPassword('');
    setConfirmPassword('');
    setShowResetPassword(false);
    setResetError(null);
  };

  const handleResetPasswordSubmit = async (e) => {
    e.preventDefault();
    if (!resetPasswordModal) return;
    setResetError(null);

    if (newPassword.length < 8) {
      setResetError('New password must be at least 8 characters in length.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setResetError('Passwords do not match. Please re-enter.');
      return;
    }

    setResetSubmitting(true);
    try {
      await api.patch(`/api/users/${resetPasswordModal.id}/reset-password`, {
        newPassword,
      });

      setFeedback({
        type: 'success',
        message: `Password reset successfully for @${resetPasswordModal.username}.`,
      });
      setResetPasswordModal(null);
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to reset user password.';
      setResetError(msg);
    } finally {
      setResetSubmitting(false);
    }
  };

  // Handle Deactivate User
  const handleOpenDeactivateModal = (targetUser) => {
    if (currentUser && String(currentUser.id) === String(targetUser.id)) {
      setFeedback({
        type: 'error',
        message: 'Administrators cannot deactivate their own administrative account.',
      });
      return;
    }
    setDeactivateModal(targetUser);
  };

  const handleConfirmDeactivate = async () => {
    if (!deactivateModal) return;
    setDeactivateSubmitting(true);

    try {
      await api.patch(`/api/users/${deactivateModal.id}/deactivate`);
      setFeedback({
        type: 'success',
        message: `Account @${deactivateModal.username} has been deactivated.`,
      });
      setDeactivateModal(null);
      fetchUsers();
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to deactivate account.';
      setFeedback({ type: 'error', message: msg });
      setDeactivateModal(null);
    } finally {
      setDeactivateSubmitting(false);
    }
  };

  // Badges & Helpers
  const getRoleBadge = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-purple-950/70 text-purple-300 border-purple-500/50 shadow-sm shadow-purple-950/30';
      case 'BASE_COMMANDER':
        return 'bg-amber-950/70 text-amber-300 border-amber-500/50 shadow-sm shadow-amber-950/30';
      case 'LOGISTICS_OFFICER':
        return 'bg-cyan-950/70 text-cyan-300 border-cyan-500/50 shadow-sm shadow-cyan-950/30';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
      });
    } catch {
      return String(isoString).split('T')[0];
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-purple-950/60 border border-purple-500/30 text-purple-400">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl md:text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                Personnel & Access Control (RBAC)
              </h1>
              <p className="text-xs font-mono text-slate-400">
                SYSTEM ADMINISTRATOR DIRECTORY • NO PUBLIC SELF-REGISTRATION
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={() => fetchUsers()}
            disabled={loading}
            className="p-2 rounded-lg bg-slate-900 border border-slate-700 text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
            title="Refresh directory"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={handleOpenCreateModal}
            className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-lg bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs uppercase font-mono tracking-wider transition-colors shadow-lg shadow-purple-950/50 border border-purple-400/30"
          >
            <UserPlus className="w-4 h-4" />
            <span>Create User</span>
          </button>
        </div>
      </div>

      {/* Feedback Toast */}
      {feedback && (
        <div
          className={`p-3.5 rounded-lg border flex items-center justify-between text-xs font-mono animate-fadeIn ${
            feedback.type === 'success'
              ? 'bg-emerald-950/40 border-emerald-500/40 text-emerald-300'
              : 'bg-rose-950/40 border-rose-500/40 text-rose-300'
          }`}
        >
          <div className="flex items-center space-x-2">
            {feedback.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            )}
            <span>{feedback.message}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            className="text-slate-400 hover:text-slate-200"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Filter Bar */}
      <div className="p-4 rounded-xl bg-[#0c1322] border border-slate-800/80 shadow-sm space-y-3">
        <div className="flex items-center space-x-2 text-xs font-mono uppercase tracking-wider text-slate-400">
          <Filter className="w-3.5 h-3.5 text-purple-400" />
          <span>Directory Filters</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 font-mono text-xs">
          {/* Role Filter */}
          <div>
            <label className="block text-slate-400 mb-1">Clearance Role</label>
            <select
              value={filterRole}
              onChange={(e) => {
                setFilterRole(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700/80 text-slate-200 focus:outline-none focus:border-purple-500"
            >
              <option value="">All Roles</option>
              <option value="ADMIN">ADMIN (Global)</option>
              <option value="BASE_COMMANDER">BASE_COMMANDER</option>
              <option value="LOGISTICS_OFFICER">LOGISTICS_OFFICER</option>
            </select>
          </div>

          {/* Base Filter */}
          <div>
            <label className="block text-slate-400 mb-1">Assigned Installation</label>
            <select
              value={filterBaseId}
              onChange={(e) => {
                setFilterBaseId(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700/80 text-slate-200 focus:outline-none focus:border-purple-500"
            >
              <option value="">All Bases</option>
              {bases.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <label className="block text-slate-400 mb-1">Account Status</label>
            <select
              value={filterActive}
              onChange={(e) => {
                setFilterActive(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700/80 text-slate-200 focus:outline-none focus:border-purple-500"
            >
              <option value="">All Statuses (Active & Inactive)</option>
              <option value="true">Active Only</option>
              <option value="false">Inactive / Deactivated Only</option>
            </select>
          </div>
        </div>
      </div>

      {/* Users Table */}
      <div className="rounded-xl border border-slate-800 overflow-hidden bg-[#0c1322] shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-slate-900/90 font-mono uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="p-3.5">Username</th>
                <th className="p-3.5">Full Name</th>
                <th className="p-3.5">Email Address</th>
                <th className="p-3.5">Clearance Role</th>
                <th className="p-3.5">Installation Base</th>
                <th className="p-3.5">Status</th>
                <th className="p-3.5">Created At</th>
                <th className="p-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
              {loading ? (
                <tr>
                  <td colSpan="8" className="p-12 text-center text-slate-500">
                    <div className="flex items-center justify-center space-x-2">
                      <RefreshCw className="w-4 h-4 animate-spin text-purple-400" />
                      <span>Loading user credentials directory...</span>
                    </div>
                  </td>
                </tr>
              ) : users.length ? (
                users.map((u) => {
                  const isOwnAccount = currentUser && String(currentUser.id) === String(u.id);
                  const active = u.isActive ?? u.is_active;

                  return (
                    <tr
                      key={u.id}
                      className={`hover:bg-slate-850/40 transition-colors ${
                        isOwnAccount ? 'bg-purple-950/10' : ''
                      }`}
                    >
                      {/* Username */}
                      <td className="p-3.5 font-medium text-emerald-400">
                        <div className="flex items-center space-x-1.5">
                          <span>@{u.username}</span>
                          {isOwnAccount && (
                            <span className="px-1.5 py-0.2 rounded text-[9px] font-mono bg-purple-900/60 text-purple-200 border border-purple-500/40">
                              YOU
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Full Name */}
                      <td className="p-3.5 text-white font-medium">
                        {u.fullName || u.full_name}
                      </td>

                      {/* Email */}
                      <td className="p-3.5 text-slate-400">{u.email}</td>

                      {/* Role */}
                      <td className="p-3.5">
                        <span
                          className={`inline-block px-2.5 py-1 rounded-md text-[10px] font-mono border ${getRoleBadge(
                            u.role
                          )}`}
                        >
                          {u.role}
                        </span>
                      </td>

                      {/* Base */}
                      <td className="p-3.5">
                        {u.baseName || u.base_name ? (
                          <span className="flex items-center text-slate-300">
                            <Building2 className="w-3.5 h-3.5 mr-1.5 text-slate-400 shrink-0" />
                            {u.baseName || u.base_name}
                          </span>
                        ) : (
                          <span className="text-slate-500 italic">
                            HQ (Global Fleet)
                          </span>
                        )}
                      </td>

                      {/* Status */}
                      <td className="p-3.5">
                        {active ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-950/60 text-emerald-400 border border-emerald-500/30">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 mr-1.5 animate-pulse"></span>
                            ACTIVE
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-mono bg-rose-950/60 text-rose-400 border border-rose-500/30">
                            <span className="w-1.5 h-1.5 rounded-full bg-rose-400 mr-1.5"></span>
                            DEACTIVATED
                          </span>
                        )}
                      </td>

                      {/* Created At */}
                      <td className="p-3.5 text-slate-400">
                        {formatDate(u.createdAt || u.created_at)}
                      </td>

                      {/* Actions */}
                      <td className="p-3.5 text-right space-x-1.5 whitespace-nowrap">
                        {/* Edit Button */}
                        <button
                          onClick={() => handleOpenEditModal(u)}
                          className="inline-flex items-center px-2.5 py-1.5 rounded bg-slate-800 text-slate-300 border border-slate-700 hover:bg-slate-700 hover:text-white text-[11px] transition-colors"
                          title="Edit user details"
                        >
                          <Edit3 className="w-3.5 h-3.5 mr-1 text-slate-400" />
                          <span>Edit</span>
                        </button>

                        {/* Reset Password Button */}
                        <button
                          onClick={() => handleOpenResetPasswordModal(u)}
                          className="inline-flex items-center px-2.5 py-1.5 rounded bg-amber-950/40 text-amber-300 border border-amber-800/40 hover:bg-amber-900/40 text-[11px] transition-colors"
                          title="Admin-initiated password reset"
                        >
                          <KeyRound className="w-3.5 h-3.5 mr-1 text-amber-400" />
                          <span>Reset Pwd</span>
                        </button>

                        {/* Deactivate Button */}
                        {isOwnAccount ? (
                          <span
                            className="inline-block relative group"
                            title="Administrators cannot deactivate their own account"
                          >
                            <button
                              disabled
                              className="inline-flex items-center px-2.5 py-1.5 rounded bg-slate-900/60 text-slate-600 border border-slate-800 text-[11px] cursor-not-allowed"
                            >
                              <UserX className="w-3.5 h-3.5 mr-1" />
                              <span>Deactivate</span>
                            </button>
                            <div className="absolute right-0 bottom-full mb-1 hidden group-hover:block z-20 w-48 p-1.5 bg-slate-900 text-rose-300 text-[10px] font-sans rounded border border-rose-900/50 shadow-lg text-center">
                              Cannot deactivate own admin account
                            </div>
                          </span>
                        ) : (
                          <button
                            onClick={() => handleOpenDeactivateModal(u)}
                            disabled={!active}
                            className={`inline-flex items-center px-2.5 py-1.5 rounded text-[11px] transition-colors ${
                              active
                                ? 'bg-rose-950/40 text-rose-300 border border-rose-800/40 hover:bg-rose-900/50'
                                : 'bg-slate-900/60 text-slate-600 border border-slate-800 cursor-not-allowed'
                            }`}
                            title={
                              active
                                ? 'Deactivate account'
                                : 'Account is already deactivated'
                            }
                          >
                            <UserX className="w-3.5 h-3.5 mr-1 text-rose-400" />
                            <span>Deactivate</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="8" className="p-12 text-center text-slate-500">
                    No users matching current filters found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 border-t border-slate-800 bg-slate-950/60 font-mono text-xs">
          <div className="text-slate-400">
            Showing{' '}
            <span className="text-white font-semibold">
              {users.length ? page * pageSize + 1 : 0}
            </span>{' '}
            to{' '}
            <span className="text-white font-semibold">
              {Math.min((page + 1) * pageSize, totalElements)}
            </span>{' '}
            of{' '}
            <span className="text-white font-semibold">{totalElements}</span>{' '}
            personnel accounts
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || loading}
              className="inline-flex items-center px-3 py-1.5 rounded-lg border border-slate-700 bg-slate-900 text-slate-300 hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeft className="w-4 h-4 mr-1" />
              <span>Previous</span>
            </button>
            <span className="px-2 text-slate-400">
              Page {totalPages > 0 ? page + 1 : 0} of {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page + 1 >= totalPages || loading}
              className="inline-flex items-center px-3 py-1.5 rounded-lg border border-slate-700 bg-slate-900 text-slate-300 hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <span>Next</span>
              <ChevronRight className="w-4 h-4 ml-1" />
            </button>
          </div>
        </div>
      </div>

      {/* CREATE USER MODAL */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-lg bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <UserPlus className="w-5 h-5 text-purple-400" />
                <span>Create User Account</span>
              </h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {createError && (
              <div className="p-3 rounded-lg border border-rose-500/40 bg-rose-950/40 text-rose-300 text-xs font-mono flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{createError}</span>
              </div>
            )}

            <form onSubmit={handleCreateSubmit} className="space-y-3 font-mono text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    Username <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. jmiller"
                    value={createUsername}
                    onChange={(e) => setCreateUsername(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500"
                  />
                </div>
                <div>
                  <label className="block text-slate-400 mb-1">
                    Full Name & Rank <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Major John Miller"
                    value={createFullName}
                    onChange={(e) => setCreateFullName(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    Email Address <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="email"
                    required
                    placeholder="jmiller@mams.mil"
                    value={createEmail}
                    onChange={(e) => setCreateEmail(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500"
                  />
                </div>
                <div>
                  <label className="block text-slate-400 mb-1">
                    Initial Password (min 8 chars) <span className="text-rose-400">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={createShowPassword ? 'text' : 'password'}
                      required
                      minLength={8}
                      placeholder="••••••••••"
                      value={createPassword}
                      onChange={(e) => setCreatePassword(e.target.value)}
                      className="w-full px-3 py-2 pr-9 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500"
                    />
                    <button
                      type="button"
                      onClick={() => setCreateShowPassword(!createShowPassword)}
                      className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-200"
                    >
                      {createShowPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    Clearance Role <span className="text-rose-400">*</span>
                  </label>
                  <select
                    value={createRole}
                    onChange={(e) => {
                      const newR = e.target.value;
                      setCreateRole(newR);
                      if (newR === 'ADMIN') {
                        setCreateBaseId('');
                      }
                    }}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500"
                  >
                    <option value="BASE_COMMANDER">BASE_COMMANDER</option>
                    <option value="LOGISTICS_OFFICER">LOGISTICS_OFFICER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-400 mb-1">
                    Assigned Base {createRole === 'ADMIN' ? '(Disabled for Admin)' : '*'}
                  </label>
                  <select
                    disabled={createRole === 'ADMIN'}
                    required={createRole !== 'ADMIN'}
                    value={createRole === 'ADMIN' ? '' : createBaseId}
                    onChange={(e) => setCreateBaseId(e.target.value)}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500 ${
                      createRole === 'ADMIN' ? 'opacity-40 cursor-not-allowed' : ''
                    }`}
                  >
                    <option value="">
                      {createRole === 'ADMIN' ? 'NULL (HQ Global)' : 'Select Base...'}
                    </option>
                    {bases.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Informative note when Admin is selected */}
              {createRole === 'ADMIN' && (
                <div className="p-2.5 rounded-lg bg-purple-950/30 border border-purple-800/40 text-[11px] text-purple-300 flex items-center space-x-2">
                  <Shield className="w-4 h-4 text-purple-400 shrink-0" />
                  <span>Admins are not tied to a specific base (global enterprise access).</span>
                </div>
              )}

              <div className="flex justify-end space-x-2 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createSubmitting}
                  className="inline-flex items-center space-x-1.5 px-4 py-2 rounded-lg bg-purple-600 hover:bg-purple-500 text-white font-bold transition-colors disabled:opacity-50"
                >
                  {createSubmitting ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      <span>Provisioning...</span>
                    </>
                  ) : (
                    <>
                      <UserPlus className="w-3.5 h-3.5" />
                      <span>Provision User</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDIT USER MODAL */}
      {editUserModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-lg bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Edit3 className="w-5 h-5 text-purple-400" />
                <span>Edit User: @{editUserModal.username}</span>
              </h2>
              <button
                onClick={() => setEditUserModal(null)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {editError && (
              <div className="p-3 rounded-lg border border-rose-500/40 bg-rose-950/40 text-rose-300 text-xs font-mono flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{editError}</span>
              </div>
            )}

            <form onSubmit={handleEditSubmit} className="space-y-3 font-mono text-xs">
              <div>
                <label className="block text-slate-400 mb-1">Username (Immutable)</label>
                <input
                  type="text"
                  disabled
                  value={editUserModal.username}
                  className="w-full px-3 py-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-500 cursor-not-allowed"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1">
                  Full Name & Rank <span className="text-rose-400">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={editFullName}
                  onChange={(e) => setEditFullName(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    Role Clearance <span className="text-rose-400">*</span>
                  </label>
                  <select
                    value={editRole}
                    disabled={currentUser && String(currentUser.id) === String(editUserModal.id)}
                    onChange={(e) => {
                      const newR = e.target.value;
                      setEditRole(newR);
                      if (newR === 'ADMIN') {
                        setEditBaseId('');
                      }
                    }}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500 disabled:opacity-40"
                  >
                    <option value="ADMIN">ADMIN</option>
                    <option value="BASE_COMMANDER">BASE_COMMANDER</option>
                    <option value="LOGISTICS_OFFICER">LOGISTICS_OFFICER</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-400 mb-1">
                    Assigned Base {editRole === 'ADMIN' ? '(Disabled for Admin)' : '*'}
                  </label>
                  <select
                    disabled={editRole === 'ADMIN'}
                    required={editRole !== 'ADMIN'}
                    value={editRole === 'ADMIN' ? '' : editBaseId}
                    onChange={(e) => setEditBaseId(e.target.value)}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-purple-500 ${
                      editRole === 'ADMIN' ? 'opacity-40 cursor-not-allowed' : ''
                    }`}
                  >
                    <option value="">
                      {editRole === 'ADMIN' ? 'NULL (HQ Global)' : 'Select Base...'}
                    </option>
                    {bases.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {editRole === 'ADMIN' && (
                <div className="p-2.5 rounded-lg bg-purple-950/30 border border-purple-800/40 text-[11px] text-purple-300 flex items-center space-x-2">
                  <Shield className="w-4 h-4 text-purple-400 shrink-0" />
                  <span>Admins are not tied to a specific base.</span>
                </div>
              )}

              <div>
                <label className="block text-slate-400 mb-1">Account Active Status</label>
                <div className="flex items-center space-x-3 mt-1">
                  <label className="inline-flex items-center space-x-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={editIsActive}
                      disabled={currentUser && String(currentUser.id) === String(editUserModal.id)}
                      onChange={(e) => setEditIsActive(e.target.checked)}
                      className="rounded bg-slate-900 border-slate-700 text-purple-600 focus:ring-purple-500"
                    />
                    <span className="text-slate-300">Account is Active & Authorized</span>
                  </label>
                </div>
                {currentUser && String(currentUser.id) === String(editUserModal.id) && (
                  <p className="text-[11px] text-amber-400 mt-1">
                    * You cannot change your own role or de-authorize your own admin account.
                  </p>
                )}
              </div>

              <div className="flex justify-end space-x-2 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setEditUserModal(null)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={editSubmitting}
                  className="inline-flex items-center space-x-1.5 px-4 py-2 rounded-lg bg-purple-600 hover:bg-purple-500 text-white font-bold transition-colors disabled:opacity-50"
                >
                  {editSubmitting ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      <span>Saving...</span>
                    </>
                  ) : (
                    <>
                      <Check className="w-3.5 h-3.5" />
                      <span>Save Changes</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* RESET PASSWORD MODAL */}
      {resetPasswordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-md bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <KeyRound className="w-5 h-5 text-amber-400" />
                <span>Reset Password: @{resetPasswordModal.username}</span>
              </h2>
              <button
                onClick={() => setResetPasswordModal(null)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="text-xs text-slate-400">
              Set a new secure password for this user. The old password is not required (admin credential override).
            </div>

            {resetError && (
              <div className="p-3 rounded-lg border border-rose-500/40 bg-rose-950/40 text-rose-300 text-xs font-mono flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{resetError}</span>
              </div>
            )}

            <form onSubmit={handleResetPasswordSubmit} className="space-y-3 font-mono text-xs">
              <div>
                <label className="block text-slate-400 mb-1">
                  New Password (min 8 characters) <span className="text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showResetPassword ? 'text' : 'password'}
                    required
                    minLength={8}
                    placeholder="Enter new password..."
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full px-3 py-2 pr-9 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-amber-500"
                  />
                  <button
                    type="button"
                    onClick={() => setShowResetPassword(!showResetPassword)}
                    className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-200"
                  >
                    {showResetPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-slate-400 mb-1">
                  Confirm New Password <span className="text-rose-400">*</span>
                </label>
                <input
                  type={showResetPassword ? 'text' : 'password'}
                  required
                  minLength={8}
                  placeholder="Re-enter new password..."
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-amber-500"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setResetPasswordModal(null)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={resetSubmitting}
                  className="inline-flex items-center space-x-1.5 px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-500 text-white font-bold transition-colors disabled:opacity-50"
                >
                  {resetSubmitting ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      <span>Updating...</span>
                    </>
                  ) : (
                    <>
                      <KeyRound className="w-3.5 h-3.5" />
                      <span>Update Password</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CONFIRM DEACTIVATE DIALOG */}
      {deactivateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-md bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center space-x-3 text-rose-400">
              <div className="p-2.5 rounded-xl bg-rose-950/60 border border-rose-500/30">
                <ShieldAlert className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Deactivate User Account</h3>
                <p className="text-xs font-mono text-slate-400">SOFT DELETE CONFIRMATION</p>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs font-mono text-slate-300 space-y-1">
              <div>
                <span className="text-slate-500">Target User:</span> @{deactivateModal.username}
              </div>
              <div>
                <span className="text-slate-500">Full Name:</span> {deactivateModal.fullName || deactivateModal.full_name}
              </div>
              <div>
                <span className="text-slate-500">Clearance:</span> {deactivateModal.role}
              </div>
            </div>

            <p className="text-xs text-slate-400 leading-relaxed">
              Deactivating this account prevents the operator from logging into MAMS.
              Historical purchases, transfers, and assignment audit logs will be permanently preserved.
            </p>

            <div className="flex justify-end space-x-2 pt-2 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setDeactivateModal(null)}
                className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-mono text-xs transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmDeactivate}
                disabled={deactivateSubmitting}
                className="inline-flex items-center space-x-1.5 px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-mono text-xs font-bold transition-colors disabled:opacity-50"
              >
                {deactivateSubmitting ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    <span>Deactivating...</span>
                  </>
                ) : (
                  <>
                    <UserX className="w-3.5 h-3.5" />
                    <span>Confirm Deactivate</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserManagementPage;
