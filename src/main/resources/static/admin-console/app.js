const state = {
  users: [],
  demoAccounts: [],
  selectedUser: null,
};

const summaryGrid = document.getElementById("summary-grid");
const userTableBody = document.getElementById("user-table-body");
const accountList = document.getElementById("account-list");
const searchInput = document.getElementById("search-input");
const statusText = document.getElementById("status-text");
const agentResult = document.getElementById("agent-result");

const form = document.getElementById("user-form");
const formTitle = document.getElementById("form-title");
const userIdInput = document.getElementById("user-id");
const phoneInput = document.getElementById("phone-input");
const nicknameInput = document.getElementById("nickname-input");
const statusInput = document.getElementById("status-input");
const passwordInput = document.getElementById("password-input");
const keepSessionsInput = document.getElementById("keep-sessions-input");

const summaryMeta = [
  { key: "userCount", label: "用户数", icon: "U", note: "账号池" },
  { key: "productCount", label: "商品数", icon: "P", note: "基础商品档案" },
  { key: "customerCount", label: "客户数", icon: "C", note: "应收主体" },
  { key: "supplierCount", label: "供应商数", icon: "S", note: "应付主体" },
  { key: "saleOrderCount", label: "销售单", icon: "SO", note: "出库历史" },
  { key: "purchaseOrderCount", label: "采购单", icon: "PO", note: "入库草稿与已收货" },
  { key: "agentTaskCount", label: "Agent 任务", icon: "AI", note: "后台深度分析" },
  { key: "unreadNotificationCount", label: "未读通知", icon: "N", note: "待回看的运行结果" },
];

async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  const payload = await response.json();
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

async function bootstrap() {
  initBackground();
  await Promise.all([loadSummary(), loadUsers(), seedDemo(false)]);
}

async function loadSummary() {
  const summary = await request("/v1/admin/summary");
  renderSummary(summary);
  statusText.textContent = "本地管理后台已连接，当前正在使用 local profile 的演示环境。";
}

async function loadUsers(keyword = "") {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
  state.users = await request(`/v1/admin/users${query}`);
  renderUsers();
}

async function seedDemo(reset) {
  const result = await request(`/v1/admin/demo/seed?reset=${reset ? "true" : "false"}`, {
    method: "POST",
  });
  state.demoAccounts = result.demoAccounts || [];
  renderAccounts();
  await Promise.all([loadSummary(), loadUsers(searchInput.value.trim())]);
  agentResult.textContent = `${reset ? "已重建" : "已补齐"}演示数据：用户 ${result.userCount}，商品 ${result.productCount}，客户 ${result.customerCount}，供应商 ${result.supplierCount}。`;
  agentResult.classList.remove("empty");
}

function renderSummary(summary) {
  summaryGrid.innerHTML = summaryMeta
    .map((item) => {
      const value = summary[item.key] ?? 0;
      return `
        <article class="summary-card">
          <div class="summary-meta">
            <div>
              <div class="summary-label">${item.label}</div>
              <div class="summary-value">${value}</div>
            </div>
            <div class="summary-icon">${item.icon}</div>
          </div>
          <div class="summary-note">${item.note}</div>
        </article>
      `;
    })
    .join("");
}

function renderAccounts() {
  accountList.innerHTML = state.demoAccounts
    .map(
      (account) => `
        <article class="account-item">
          <strong>${escapeHtml(account.nickname)}</strong>
          <span>${escapeHtml(account.phone)}</span>
          <code>${escapeHtml(account.phone)} / ${escapeHtml(account.password)}</code>
        </article>
      `,
    )
    .join("");
}

function renderUsers() {
  userTableBody.innerHTML = state.users
    .map((user) => {
      const statusClass = user.status === 1 ? "status-pill" : "status-pill off";
      const statusTextLabel = user.status === 1 ? "启用" : "停用";
      return `
        <tr>
          <td>${user.id}</td>
          <td>${escapeHtml(user.phone)}</td>
          <td>${escapeHtml(user.nickname)}</td>
          <td><span class="${statusClass}">${statusTextLabel}</span></td>
          <td>${user.activeSessions}</td>
          <td>${formatTime(user.createdAt)}</td>
          <td><button class="link-button" data-user-id="${user.id}">编辑</button></td>
        </tr>
      `;
    })
    .join("");

  userTableBody.querySelectorAll("[data-user-id]").forEach((button) => {
    button.addEventListener("click", () => {
      const user = state.users.find((item) => String(item.id) === button.dataset.userId);
      if (user) {
        fillForm(user);
      }
    });
  });
}

function fillForm(user) {
  state.selectedUser = user;
  formTitle.textContent = `编辑用户 #${user.id}`;
  userIdInput.value = user.id;
  phoneInput.value = user.phone;
  phoneInput.disabled = true;
  nicknameInput.value = user.nickname;
  statusInput.value = String(user.status);
  passwordInput.value = "";
  keepSessionsInput.checked = true;
}

function resetForm() {
  state.selectedUser = null;
  formTitle.textContent = "新建用户";
  userIdInput.value = "";
  phoneInput.value = "";
  phoneInput.disabled = false;
  nicknameInput.value = "";
  statusInput.value = "1";
  passwordInput.value = "";
  keepSessionsInput.checked = true;
}

async function handleFormSubmit(event) {
  event.preventDefault();
  const userId = userIdInput.value.trim();
  const payload = {
    nickname: nicknameInput.value.trim(),
    status: Number(statusInput.value),
    password: passwordInput.value.trim(),
    keepSessions: keepSessionsInput.checked,
  };

  if (userId) {
    await request(`/v1/admin/users/${userId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  } else {
    await request("/v1/admin/users", {
      method: "POST",
      body: JSON.stringify({
        phone: phoneInput.value.trim(),
        password: passwordInput.value.trim() || "123456",
        nickname: nicknameInput.value.trim(),
        status: Number(statusInput.value),
      }),
    });
  }

  await loadUsers(searchInput.value.trim());
  await loadSummary();
  resetForm();
}

function formatTime(timestamp) {
  return new Date(timestamp).toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function initBackground() {
  const canvas = document.getElementById("bg-canvas");
  if (!(canvas instanceof HTMLCanvasElement)) {
    return;
  }
  const ctx = canvas.getContext("2d");
  if (!ctx) {
    return;
  }

  const particles = [];
  const mouse = { x: 0, y: 0 };

  function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    particles.length = 0;
    const count = Math.max(36, Math.floor((canvas.width * canvas.height) / 30000));
    for (let index = 0; index < count; index += 1) {
      particles.push({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height,
        vx: (Math.random() - 0.5) * 0.2,
        vy: (Math.random() - 0.5) * 0.2,
        size: Math.random() * 1.6 + 0.5,
        opacity: Math.random() * 0.25 + 0.06,
      });
    }
  }

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const bg = ctx.createRadialGradient(
      canvas.width * 0.3,
      canvas.height * 0.2,
      0,
      canvas.width * 0.5,
      canvas.height * 0.5,
      canvas.width * 0.85,
    );
    bg.addColorStop(0, "rgba(24, 38, 70, 0.82)");
    bg.addColorStop(0.55, "rgba(10, 18, 34, 0.92)");
    bg.addColorStop(1, "rgba(5, 10, 18, 1)");
    ctx.fillStyle = bg;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    for (let index = 0; index < particles.length; index += 1) {
      const particle = particles[index];
      particle.x += particle.vx;
      particle.y += particle.vy;

      if (particle.x < 0 || particle.x > canvas.width) {
        particle.vx *= -1;
      }
      if (particle.y < 0 || particle.y > canvas.height) {
        particle.vy *= -1;
      }

      const glow = ctx.createRadialGradient(particle.x, particle.y, 0, particle.x, particle.y, particle.size * 6);
      glow.addColorStop(0, `rgba(132, 176, 255, ${particle.opacity})`);
      glow.addColorStop(1, "rgba(132, 176, 255, 0)");
      ctx.fillStyle = glow;
      ctx.beginPath();
      ctx.arc(particle.x, particle.y, particle.size * 6, 0, Math.PI * 2);
      ctx.fill();

      for (let inner = index + 1; inner < particles.length; inner += 1) {
        const next = particles[inner];
        const dx = particle.x - next.x;
        const dy = particle.y - next.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 140) {
          continue;
        }
        const mouseDistance = Math.sqrt((particle.x - mouse.x) ** 2 + (particle.y - mouse.y) ** 2);
        const opacity = (1 - distance / 140) * (mouseDistance < 220 ? 0.14 : 0.06);
        ctx.strokeStyle = `rgba(116, 165, 255, ${opacity})`;
        ctx.lineWidth = 0.8;
        ctx.beginPath();
        ctx.moveTo(particle.x, particle.y);
        ctx.lineTo(next.x, next.y);
        ctx.stroke();
      }
    }

    requestAnimationFrame(draw);
  }

  window.addEventListener("resize", resize);
  window.addEventListener("mousemove", (event) => {
    mouse.x = event.clientX;
    mouse.y = event.clientY;
  });
  resize();
  draw();
}

document.getElementById("seed-reset-button").addEventListener("click", () => seedDemo(true).catch(handleError));
document.getElementById("seed-button").addEventListener("click", () => seedDemo(false).catch(handleError));
document.getElementById("search-button").addEventListener("click", () => loadUsers(searchInput.value.trim()).catch(handleError));
document.getElementById("refresh-button").addEventListener("click", () => bootstrap().catch(handleError));
document.getElementById("new-user-button").addEventListener("click", resetForm);
document.getElementById("reset-form-button").addEventListener("click", resetForm);
form.addEventListener("submit", (event) => handleFormSubmit(event).catch(handleError));

function handleError(error) {
  console.error(error);
  agentResult.textContent = `操作失败：${error.message}`;
  agentResult.classList.remove("empty");
}

bootstrap().catch(handleError);
