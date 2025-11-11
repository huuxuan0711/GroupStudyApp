import { onValueCreated, onValueDeleted, onValueWritten, onValueUpdated } from "firebase-functions/v2/database";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.database();
const fcm = admin.messaging();

/* -------------------------------------------------------------------------- */
/* 💾 Lưu thông báo và trả về id                                              */
/* -------------------------------------------------------------------------- */
async function saveUserNotifications(
  userIds,
  type,
  data,
  title,
  message
) {
  const notifIds = [];

  for (const userId of userIds) {
    const notifRef = db.ref(`notifications/${userId}`).push(); // nhánh riêng cho từng user
    const notificationData = {
      id: notifRef.key,
      userId,
      groupId: data.groupId || "",
      taskId: data.taskId || "",
      inviteCode: data.inviteCode || "",
      title,
      message,
      type,
      timestamp: Date.now(),
      read: false,
    };

    await notifRef.set(notificationData);
    notifIds.push({ userId, id: notifRef.key });
  }


  logger.info(`💾 Đã lưu ${userIds.length} thông báo [${type}]: ${title}`);
  return notifIds; // Trả về danh sách id để gửi kèm
}

/* -------------------------------------------------------------------------- */
/* 🔔 Tạo payload FCM theo type                                               */
/* -------------------------------------------------------------------------- */
function createNotificationPayload(type, data, title, message, notificationId) {
  let payloadData = { type, notificationId };

  if (type === "task" || type === "message" || type === "member" || type === "file") {
    payloadData.groupId = data.groupId;
  } else if (type === "invite") {
    payloadData.inviteCode = data.inviteCode;
  } else if (type === "deadline") {
    payloadData.taskId = data.taskId;
  }

  return {
    notification: { title, body: message },
    data: payloadData,
  };
}

/* -------------------------------------------------------------------------- */
/* 🔔 Gửi notification đến nhiều user                                         */
/* -------------------------------------------------------------------------- */
async function sendNotification(userIds, type, data, title, message) {
  const tokensSnap = await db.ref("UserTokens").once("value");
  const allTokens = tokensSnap.val() || {};

  const tokens = [];
  for (const uid of userIds) {
    const token = allTokens[uid];
    if (token) tokens.push(token);
  }

  if (tokens.length === 0) return;

  // Lưu notification vào DB trước để có id
  const notifIds = await saveUserNotifications(userIds, type, data, title, message);

  // Gửi lần lượt theo từng user (để gửi đúng id)
  for (const { userId, id } of notifIds) {
    const token = allTokens[userId];
    if (!token) continue;

    const payload = createNotificationPayload(type, data, title, message, id);
    await fcm.sendEachForMulticast({
      notification: payload.notification,
      data: payload.data,
      tokens: [token],
    });
  }
}

/* -------------------------------------------------------------------------- */
/* 🧩 Task mới hoặc cập nhật                                                  */
/* -------------------------------------------------------------------------- */
export const onTaskWrite = onValueWritten("/tasks/{taskId}", async (event) => {
  const task = event.data.after.val();
  const prev = event.data.before.val();
  if (!task) return null;

  const groupId = task.groupId;
  const creatorId = task.createdBy;
  const assignedUserIds = task.status ? Object.keys(task.status) : [];

  let title = "Cập nhật nhiệm vụ";
  let message = "";

  if (!prev) {
    message = `Một nhiệm vụ mới được tạo: ${task.title}`;
  } else if (JSON.stringify(task.status) !== JSON.stringify(prev.status)) {
    message = `Trạng thái nhiệm vụ "${task.title}" vừa được cập nhật.`;
  } else {
    message = `Nhiệm vụ "${task.title}" vừa được chỉnh sửa.`;
  }

  const receivers = assignedUserIds.filter((id) => id !== creatorId);

  await sendNotification(receivers, "task", { groupId, taskId: task.id }, title, message);
  return null;
});

/* -------------------------------------------------------------------------- */
/* 💬 Tin nhắn mới trong group                                                */
/* -------------------------------------------------------------------------- */
export const onMessageCreate = onValueCreated("/messages/{messageId}", async (event) => {
  const msg = event.data.val();
  if (!msg) return null;

  const groupId = msg.groupId;
  const receiversSnap = await db.ref("members").orderByChild("groupId").equalTo(groupId).once("value");
  const members = receiversSnap.val() || {};
  const memberIds = Object.values(members)
    .map((m) => m.userId)
    .filter((id) => id !== msg.senderId);

  const title = "Tin nhắn mới";
  const message = `${msg.memberName || "Thành viên"}: ${msg.text || "đã gửi một tệp tin"}`;

  await sendNotification(memberIds, "message", { groupId }, title, message);
  return null;
});

/* -------------------------------------------------------------------------- */
/* 👥 Thành viên mới tham gia                                                 */
/* -------------------------------------------------------------------------- */
export const onMemberJoin = onValueCreated("/members/{memberId}", async (event) => {
  const member = event.data.val();
  if (!member) return null;

  const groupId = member.groupId;
  const receiversSnap = await db.ref("members").orderByChild("groupId").equalTo(groupId).once("value");
  const members = receiversSnap.val() || {};
  const memberIds = Object.values(members)
    .map((m) => m.userId)
    .filter((id) => id !== member.userId);

  const title = "Thành viên mới";
  const message = `${member.memberName || "Một người dùng"} vừa tham gia nhóm!`;

  await sendNotification(memberIds, "member", { groupId }, title, message);
  return null;
});

/* -------------------------------------------------------------------------- */
/* 📁 File mới được tải lên                                                   */
/* -------------------------------------------------------------------------- */
export const onFileUpload = onValueCreated("/files/{fileId}", async (event) => {
  const file = event.data.val();
  if (!file) return null;

  const groupId = file.groupId;
  const receiversSnap = await db.ref("members").orderByChild("groupId").equalTo(groupId).once("value");
  const members = receiversSnap.val() || {};
  const memberIds = Object.values(members)
    .map((m) => m.userId)
    .filter((id) => id !== file.uploadedBy);

  const title = "Tệp mới";
  const message = `${file.uploadedByName || "Ai đó"} đã tải lên: ${file.name || "tệp tin mới"}`;

  await sendNotification(memberIds, "file", { groupId }, title, message);
  return null;
});

/* -------------------------------------------------------------------------- */
/* 💌 Lời mời tham gia nhóm                                                   */
/* -------------------------------------------------------------------------- */
export const onGroupInvite = onValueCreated("/groupInvites/{inviteId}", async (event) => {
  const invite = event.data.val();
  if (!invite) return null;

  const { groupName, inviterName, inviteeId, inviteCode } = invite;

  const title = "Lời mời tham gia nhóm";
  const message = `${inviterName} đã mời bạn tham gia nhóm "${groupName}".`;

  await sendNotification([inviteeId], "invite", { inviteCode }, title, message);
  return null;
});

/* -------------------------------------------------------------------------- */
/* 🤝 Khi user chấp nhận lời mời                                             */
/* -------------------------------------------------------------------------- */
export const onInviteAccepted = onValueUpdated("/groupInvites/{inviteId}", async (event) => {
  const after = event.data.after.val();
  if (!after || after.status !== "accepted") return null;

  const { groupId, inviteeId, inviteeName } = after;

  // Thêm vào nhóm
  await db.ref(`/members/${groupId}/${inviteeId}`).set({
    name: inviteeName,
    joinedAt: Date.now(),
  });

  const receiversSnap = await db.ref(`/members/${groupId}`).once("value");
  const members = receiversSnap.val() || {};
  const memberIds = Object.keys(members).filter((id) => id !== inviteeId);

  const title = "Thành viên mới";
  const message = `${inviteeName} đã tham gia nhóm.`;

  await sendNotification(memberIds, "member", { groupId }, title, message);
  return null;
});

/* -------------------------------------------------------------------------- */
/* 🕒 Kiểm tra deadline nhiệm vụ                                              */
/* -------------------------------------------------------------------------- */
export const checkTaskDeadlines = onSchedule(
  { schedule: "every day 00:00", timeZone: "Asia/Ho_Chi_Minh" },
  async () => {
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const THREE_DAYS = 3 * 24 * 60 * 60 * 1000;

    try {
      const tasksSnap = await db.ref("/tasks").once("value");
      const tasks = tasksSnap.val() || {};
      const tokensSnap = await db.ref("UserTokens").once("value");
      const allTokens = tokensSnap.val() || {};

      for (const [taskId, task] of Object.entries(tasks)) {
        if (!task.dateOnly) continue;
        const deadlineNotified = task.deadlineNotified || {};

        for (const [userId, dateStr] of Object.entries(task.dateOnly)) {
          if (deadlineNotified[userId]) continue;

          const [day, month, year] = dateStr.split("/").map(Number);
          const deadline = new Date(year, month - 1, day).getTime();
          if (deadline <= today) continue;
          const diff = deadline - today;
          if (diff > 0 && diff <= THREE_DAYS) {
            const title = "Sắp đến hạn nhiệm vụ";
            const message = `Nhiệm vụ "${task.title}" sẽ đến hạn vào ngày ${dateStr}.`;
            const token = allTokens[userId];

            // 🔹 Lưu thông báo để lấy id
            const notifIds = await saveUserNotifications(
              [userId],
              "deadline",
              { taskId, groupId: task.groupId },
              title,
              message
            );
            const notificationId = notifIds[0]?.id;

            if (token) {
              await fcm.sendEachForMulticast({
                notification: { title, body: message },
                tokens: [token],
                data: { type: "deadline", taskId, notificationId },
              });
            }
            await db.ref(`/tasks/${taskId}/deadlineNotified/${userId}`).set(true);
          }
        }
      }
    } catch (error) {
      logger.error("❌ Lỗi kiểm tra deadline:", error);
    }
  }
);
