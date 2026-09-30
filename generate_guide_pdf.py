import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.units import inch
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_number(num_pages)
            super().showPage()
        super().save()

    def draw_page_number(self, page_count):
        self.saveState()
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#64748b"))
        
        # Header (pages 2+)
        if self._pageNumber > 1:
            self.drawString(54, 750, "DEFENSE LOGISTICS COMMAND  •  MAMS OPERATIONAL MANUAL")
            self.drawRightString(612 - 54, 750, "SECURITY: CLASSIFIED INTERNAL")
            self.setStrokeColor(colors.HexColor("#cbd5e1"))
            self.setLineWidth(0.75)
            self.line(54, 742, 612 - 54, 742)

        # Footer (all pages)
        self.setFont("Helvetica", 8)
        self.drawString(54, 36, "CONFIDENTIAL  •  MILITARY ASSET MANAGEMENT SYSTEM (MAMS)")
        page_str = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(612 - 54, 36, page_str)
        self.setStrokeColor(colors.HexColor("#cbd5e1"))
        self.setLineWidth(0.75)
        self.line(54, 48, 612 - 54, 48)
        self.restoreState()

def build_pdf(filename="MAMS_System_Guide.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()

    # Custom styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=28,
        textColor=colors.HexColor("#0f172a"),
        spaceAfter=6
    )

    subtitle_style = ParagraphStyle(
        'DocSubTitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=12,
        leading=16,
        textColor=colors.HexColor("#059669"),
        spaceAfter=15
    )

    h1_style = ParagraphStyle(
        'Heading1Custom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=colors.HexColor("#0f172a"),
        spaceBefore=14,
        spaceAfter=6,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'Heading2Custom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=15,
        textColor=colors.HexColor("#1e293b"),
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'BodyCustom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=colors.HexColor("#334155"),
        spaceAfter=6
    )

    bullet_style = ParagraphStyle(
        'BulletCustom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#334155"),
        leftIndent=15,
        firstLineIndent=-10,
        spaceAfter=3
    )

    code_style = ParagraphStyle(
        'CodeCustom',
        parent=styles['Normal'],
        fontName='Courier',
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor("#0f172a")
    )

    badge_style = ParagraphStyle(
        'BadgeCustom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=10,
        textColor=colors.white
    )

    story = []

    # ================= COVER / HEADER BANNER =================
    banner_data = [
        [Paragraph("<font color='#10b981'><b>DEFENSE LOGISTICS & MATERIEL COMMAND</b></font><br/><font size='7' color='#94a3b8'>MAMS STANDARD OPERATING PROCEDURE • SYSTEM ARCHITECTURE & USER MANUAL</font>", badge_style)]
    ]
    banner_table = Table(banner_data, colWidths=[504])
    banner_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#0c1322")),
        ('PADDING', (0, 0), (-1, -1), 10),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 10),
    ]))
    story.append(banner_table)
    story.append(Spacer(1, 14))

    story.append(Paragraph("Military Asset Management System (MAMS)", title_style))
    story.append(Paragraph("Comprehensive System Manual • End-to-End Workflow • Role Security Clearance", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor("#e2e8f0"), spaceBefore=2, spaceAfter=12))

    # ================= SECTION 1: WHAT IS MAMS? =================
    story.append(Paragraph("1. Executive Overview & System Purpose", h1_style))
    story.append(Paragraph(
        "The <b>Military Asset Management System (MAMS)</b> is a secure enterprise defense application designed for "
        "military commanders and logistics officers to track, manage, and audit critical assets—including <b>heavy vehicles, "
        "weapons, ammunition, and tactical protective gear</b>—across distributed bases with strict accountability and Zero-Trust Role-Based Access Control (RBAC).",
        body_style
    ))
    story.append(Paragraph(
        "<b>Core Problems Solved:</b>",
        h2_style
    ))
    story.append(Paragraph("• <b>Inventory Drift & Phantom Stock:</b> Traditional spreadsheets lead to inaccurate counts during deployments.", bullet_style))
    story.append(Paragraph("• <b>Accountability & Chain of Custody:</b> Assets must be traceable from procurement, through inter-base transfers, down to the exact soldier or officer in custody.", bullet_style))
    story.append(Paragraph("• <b>Operational Readiness:</b> Commanders need instant derived visibility into opening balances, daily net movements, and expended combat materials.", bullet_style))
    story.append(Paragraph("• <b>Tamper-Proof Audit Integrity:</b> Financial ledger immutability prevents unauthorized record alteration or deletion.", bullet_style))

    story.append(Spacer(1, 8))

    # ================= SECTION 2: SYSTEM ARCHITECTURE =================
    story.append(Paragraph("2. Technology Stack & Key Architectural Decisions", h1_style))
    
    tech_data = [
        [Paragraph("<b>Component</b>", body_style), Paragraph("<b>Technology</b>", body_style), Paragraph("<b>Architectural Responsibility</b>", body_style)],
        [Paragraph("<b>Backend API</b>", body_style), Paragraph("Java 17 / Spring Boot 3.3.4", body_style), Paragraph("Stateless REST APIs, Spring Security 6, transactional business logic, Flyway DB migrations.", body_style)],
        [Paragraph("<b>Database</b>", body_style), Paragraph("MySQL 8.0 (InnoDB)", body_style), Paragraph("Relational integrity with foreign keys, checks, unique constraints, and ACID transactions.", body_style)],
        [Paragraph("<b>Security & Auth</b>", body_style), Paragraph("JWT (Stateless Bearer)", body_style), Paragraph("384-bit HMAC-SHA signing, UserPrincipal context, method-level @PreAuthorize.", body_style)],
        [Paragraph("<b>Audit Trail</b>", body_style), Paragraph("Spring AOP (AspectJ)", body_style), Paragraph("Automatic interception of all mutating actions, logging IP, user, endpoint, and status.", body_style)],
        [Paragraph("<b>Frontend UI</b>", body_style), Paragraph("React 19 / Vite / Tailwind", body_style), Paragraph("Modern Tactical Command dashboard, role-aware routing, modal dialogs, and filters.", body_style)],
    ]
    tech_table = Table(tech_data, colWidths=[110, 140, 254])
    tech_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#f1f5f9")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
        ('LEFTPADDING', (0, 0), (-1, -1), 6),
        ('RIGHTPADDING', (0, 0), (-1, -1), 6),
    ]))
    story.append(tech_table)

    story.append(Spacer(1, 10))

    # ================= SECTION 3: THE GOLDEN ACCOUNTING RULE =================
    story.append(Paragraph("3. The Core Balance Calculation Engine (Critical Rule)", h1_style))
    story.append(Paragraph(
        "A foundational principle of MAMS is that <b>asset balances are DERIVED in real-time</b> and <i>never stored as a mutable counter</i>. "
        "Storing mutable counters introduces balance drift, race conditions, and ledger corruption. Instead, balances are calculated on demand "
        "from immutable ledger transactions:",
        body_style
    ))

    formula_data = [
        [Paragraph("<b>Formula Component</b>", body_style), Paragraph("<b>Mathematical Definition</b>", body_style)],
        [Paragraph("<b>Net Movement</b>", body_style), Paragraph("<font color='#0284c7'><b>net_movement = purchases + transfers_in - transfers_out</b></font>", body_style)],
        [Paragraph("<b>Opening Balance</b>", body_style), Paragraph("<font color='#475569'><b>opening_balance = closing_balance as of (start_date - 1 day)</b></font><br/>Cumulative sum of all past purchases + transfers_in - transfers_out - expended prior to the date range.", body_style)],
        [Paragraph("<b>Closing Balance</b>", body_style), Paragraph("<font color='#16a34a'><b>closing_balance = opening_balance + net_movement - expended</b></font>", body_style)],
        [Paragraph("<b>Assigned Count</b>", body_style), Paragraph("Active custody count: items currently assigned to personnel that have not yet been expended or returned.", body_style)],
    ]
    formula_table = Table(formula_data, colWidths=[130, 374])
    formula_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#f8fafc")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
    ]))
    story.append(formula_table)

    story.append(PageBreak())

    # ================= SECTION 4: THE 5 FEATURE MODULES =================
    story.append(Paragraph("4. Step-by-Step Feature Walkthrough", h1_style))

    story.append(Paragraph("Step 1 — Authentication & Session Management", h2_style))
    story.append(Paragraph(
        "Users authenticate via <b>POST /api/auth/login</b> using username and password. Upon successful validation, "
        "the backend issues a signed JWT containing <i>userId</i>, <i>role</i>, <i>baseId</i>, and <i>baseName</i>. "
        "The frontend stores this token and includes it in all subsequent requests via Axios HTTP interceptors. "
        "Zero public registration exists—all accounts are strictly provisioned by an Administrator.",
        body_style
    ))

    story.append(Paragraph("Step 2 — Command Dashboard & Net Movement Popup", h2_style))
    story.append(Paragraph(
        "The dashboard presents high-level operational intelligence for any selected date range and base. "
        "Commanders can click the <b>Net Movement</b> KPI card to open an interactive modal breakdown showing every "
        "inbound purchase shipment and transfer with timestamps, equipment names, and source/destination bases.",
        body_style
    ))

    story.append(Paragraph("Step 3 — Purchases & Procurement Ledger", h2_style))
    story.append(Paragraph(
        "Authorized personnel record new asset deliveries into a base. Each purchase records quantity, unit cost, "
        "total cost, vendor, purchase date, and notes. "
        "<b>Ledger Immutability:</b> PUT and DELETE endpoints are intentionally omitted to preserve financial ledger "
        "and legal audit integrity.",
        body_style
    ))

    story.append(Paragraph("Step 4 — Inter-Base Asset Transfers", h2_style))
    story.append(Paragraph(
        "Enables moving assets between military installations with a strict 4-state lifecycle machine: "
        "<b>pending → in_transit → completed / cancelled</b>. "
        "Non-admins can only initiate or manage transfers where their assigned installation is either the sender or receiver.",
        body_style
    ))

    story.append(Paragraph("Step 5 — Personnel Assignments & Expenditures", h2_style))
    story.append(Paragraph(
        "Tracks physical custody when equipment is checked out to soldiers and officers. "
        "Assets transition through 3 statuses: <b>assigned</b> (in personal custody) → <b>expended</b> (consumed in firing drills/combat) "
        "or <b>returned</b> (returned to armory after inspection). "
        "<b>Strict Policy:</b> Logistics Officers are completely barred from this module at both API and UI levels.",
        body_style
    ))

    story.append(Paragraph("Step 6 — Personnel Identity & Access Control (User Management)", h2_style))
    story.append(Paragraph(
        "An Administrator-only console to create accounts, update clearance roles, reset passwords without old credentials, "
        "and soft-deactivate operators. A critical safety guard prohibits administrators from deactivating or demoting their own account.",
        body_style
    ))

    story.append(Spacer(1, 8))

    # ================= SECTION 5: ROLE PERMISSIONS MATRIX =================
    story.append(Paragraph("5. Role-Based Access Control (RBAC) Clearance Matrix", h1_style))
    story.append(Paragraph(
        "MAMS enforces strict least-privilege security across three distinct military personnel tiers:",
        body_style
    ))

    rbac_data = [
        [Paragraph("<b>Capability / Permission</b>", body_style), Paragraph("<b>ADMIN</b>", body_style), Paragraph("<b>BASE_COMMANDER</b>", body_style), Paragraph("<b>LOGISTICS_OFFICER</b>", body_style)],
        [Paragraph("Base Visibility Scope", body_style), Paragraph("Global (All Bases)", body_style), Paragraph("Assigned Base Only", body_style), Paragraph("Assigned Base Only", body_style)],
        [Paragraph("View Dashboard Metrics", body_style), Paragraph("Full Access (Any Base)", body_style), Paragraph("Own Base Metrics Only", body_style), Paragraph("Own Base Metrics Only", body_style)],
        [Paragraph("Record New Purchases", body_style), Paragraph("For Any Base", body_style), Paragraph("For Own Base Only", body_style), Paragraph("For Own Base Only", body_style)],
        [Paragraph("View Purchases History", body_style), Paragraph("Enterprise-wide", body_style), Paragraph("Own Base Only", body_style), Paragraph("Own Base Only", body_style)],
        [Paragraph("Initiate Inter-Base Transfers", body_style), Paragraph("Between Any Two Bases", body_style), Paragraph("Must Involve Own Base", body_style), Paragraph("Must Involve Own Base", body_style)],
        [Paragraph("Update Transfer Status", body_style), Paragraph("Allowed", body_style), Paragraph("Allowed (Own Base)", body_style), Paragraph("Allowed (Own Base)", body_style)],
        [Paragraph("Assign Assets to Personnel", body_style), Paragraph("Allowed (All Bases)", body_style), Paragraph("Allowed (Own Base)", body_style), Paragraph("<font color='#dc2626'><b>DENIED (HTTP 403)</b></font>", body_style)],
        [Paragraph("Mark Expended / Returned", body_style), Paragraph("Allowed (All Bases)", body_style), Paragraph("Allowed (Own Base)", body_style), Paragraph("<font color='#dc2626'><b>DENIED (HTTP 403)</b></font>", body_style)],
        [Paragraph("User Management Console", body_style), Paragraph("Full Admin Console", body_style), Paragraph("<font color='#dc2626'><b>DENIED (HTTP 403)</b></font>", body_style), Paragraph("<font color='#dc2626'><b>DENIED (HTTP 403)</b></font>", body_style)],
    ]
    rbac_table = Table(rbac_data, colWidths=[164, 110, 115, 115])
    rbac_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#0f172a")),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 5),
    ]))
    story.append(rbac_table)

    story.append(PageBreak())

    # ================= SECTION 6: QUICKSTART & DEMO GUIDE =================
    story.append(Paragraph("6. Ready-to-Use Test Accounts & Verification Guide", h1_style))
    story.append(Paragraph(
        "To verify all features and role restrictions immediately, log in to <b>http://localhost:3000</b> using these seeded accounts:",
        body_style
    ))

    creds_data = [
        [Paragraph("<b>Role</b>", body_style), Paragraph("<b>Username</b>", body_style), Paragraph("<b>Password</b>", body_style), Paragraph("<b>Assigned Installation</b>", body_style), Paragraph("<b>Expected UI Behavior</b>", body_style)],
        [Paragraph("<font color='#7c3aed'><b>ADMIN</b></font>", body_style), Paragraph("admin", code_style), Paragraph("Admin@123", code_style), Paragraph("Global (None)", body_style), Paragraph("All 5 menu tabs visible. Global base dropdowns. Access to User Management.", body_style)],
        [Paragraph("<font color='#d97706'><b>BASE_COMMANDER</b></font>", body_style), Paragraph("johndoe", code_style), Paragraph("Password@123", code_style), Paragraph("Fort Alpha", body_style), Paragraph("Can assign and expend assets. Scoped to Fort Alpha. User Management hidden.", body_style)],
        [Paragraph("<font color='#0891b2'><b>LOGISTICS_OFFICER</b></font>", body_style), Paragraph("jmiler", code_style), Paragraph("Password@123", code_style), Paragraph("Fort Alpha", body_style), Paragraph("Assignments & Users tabs HIDDEN and blocked with 403 on direct URL.", body_style)],
    ]
    creds_table = Table(creds_data, colWidths=[100, 70, 80, 84, 170])
    creds_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#f1f5f9")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
        ('LEFTPADDING', (0, 0), (-1, -1), 5),
    ]))
    story.append(creds_table)

    story.append(Spacer(1, 10))

    story.append(Paragraph("7. Guided 5-Minute Testing Walkthrough", h1_style))
    
    steps = [
        ("Step 1: Inspect the Command Dashboard", "Log in as <b>admin</b>. Notice the live derived balances: Opening Balance (25,365), Net Movement (+10,107), Closing Balance (31,772). Click the <b>Net Movement</b> card to see the inbound/outbound breakdown."),
        ("Step 2: Record an Asset Purchase", "Navigate to <b>Purchases</b>. Click <b>'Record Purchase'</b>. Buy 20 M4 Carbines for Fort Alpha from Colt Defense. Watch the table update immediately."),
        ("Step 3: Dispatch an Inter-Base Transfer", "Navigate to <b>Transfers</b>. Transfer 5 Humvees from Fort Alpha to Fort Bravo. Transition the status from <i>pending</i> to <i>in_transit</i>, then <i>completed</i>."),
        ("Step 4: Arm Personnel & Fire Ammunition", "Log in as <b>johndoe</b> (Base Commander). Go to <b>Assignments & Expenditures</b>. Assign 500 rounds of ammo to a Sergeant. Click <b>'Expend'</b> to log live-fire usage. Check the Dashboard: the Expended counter increments automatically."),
        ("Step 5: Verify Logistics Officer Defense Guard", "Log in as <b>jmiler</b> (Logistics Officer). Notice that <b>Assignments</b> and <b>User Management</b> are completely missing from the sidebar. Manually navigating to <i>/assignments</i> redirects to <i>/dashboard</i>."),
        ("Step 6: User Administration & Lockout Safety", "Log back in as <b>admin</b>. Open <b>User Management</b>. Try deactivating your own account: the Deactivate button is disabled with a safety tooltip preventing administrative lockout.")
    ]

    for title, desc in steps:
        story.append(Paragraph(f"• <b>{title}:</b> {desc}", bullet_style))

    story.append(Spacer(1, 10))

    # ================= SECTION 8: SYSTEM ARCHITECTURE SUMMARY =================
    story.append(Paragraph("8. Database Entity Relationship Overview", h1_style))
    
    schema_data = [
        [Paragraph("<b>Entity Table</b>", body_style), Paragraph("<b>Key Columns & Foreign Keys</b>", body_style), Paragraph("<b>Purpose</b>", body_style)],
        [Paragraph("<b>bases</b>", body_style), Paragraph("id, name, location, is_active", body_style), Paragraph("Physical military installations (Fort Alpha, Fort Bravo, Fort Charlie)", body_style)],
        [Paragraph("<b>equipment_types</b>", body_style), Paragraph("id, name, category, unit", body_style), Paragraph("Standard catalog (vehicle, weapon, ammunition, other)", body_style)],
        [Paragraph("<b>users</b>", body_style), Paragraph("id, username, email, password_hash, role, base_id, is_active", body_style), Paragraph("Operators, credentials, and base associations", body_style)],
        [Paragraph("<b>purchases</b>", body_style), Paragraph("id, base_id, equipment_type_id, quantity, unit_cost, total_cost, vendor, purchase_date, created_by", body_style), Paragraph("Immutable ledger of inbound procurements", body_style)],
        [Paragraph("<b>transfers</b>", body_style), Paragraph("id, from_base_id, to_base_id, equipment_type_id, quantity, status, transfer_date, created_by", body_style), Paragraph("Inter-base logistics tracking with state machine", body_style)],
        [Paragraph("<b>assignment_expenditures</b>", body_style), Paragraph("id, base_id, equipment_type_id, personnel_name, personnel_id_no, quantity, status, assigned_date, expended_date, returned_date, created_by", body_style), Paragraph("Armory checkouts, personal custody, and ammunition consumption", body_style)],
        [Paragraph("<b>audit_logs</b>", body_style), Paragraph("id, user_id, action, entity_type, entity_id, method, endpoint, status_code, details_json, ip_address, timestamp", body_style), Paragraph("AOP automated compliance and security log", body_style)],
    ]
    schema_table = Table(schema_data, colWidths=[120, 204, 180])
    schema_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#f8fafc")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 5),
    ]))
    story.append(schema_table)

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Successfully generated {filename}")

if __name__ == "__main__":
    out = "MAMS_System_Guide.pdf"
    if len(sys.argv) > 1:
        out = sys.argv[1]
    build_pdf(out)
