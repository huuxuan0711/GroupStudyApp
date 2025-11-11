package com.xmobile.project1groupstudyappnew.helper.task_helper

import android.content.res.ColorStateList
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityTaskDetailBinding
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import kotlin.collections.get

class TaskDetailUIHelper(
    private val binding: ActivityTaskDetailBinding,
    private val userId: String?
) {

    fun displayData(task: Task?, group: Group?, isCreateTask: Boolean, isOwner: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.layoutRoot.visibility = View.VISIBLE

        if (isCreateTask) {
            showCreateTaskUI(group)
        } else {
            task?.let { showTaskUI(it, group, isOwner) }
        }
    }

    private fun showCreateTaskUI(group: Group?) {
        binding.taskTitle.text = binding.root.context.getString(R.string.add_task)
        binding.taskState.visibility = View.GONE
        binding.btnAction.text = binding.root.context.getString(R.string.create)
        binding.btnAction.setTextColor(binding.root.context.getColor(R.color.red))
        binding.cardCreate.visibility = View.VISIBLE
        binding.cardContent.visibility = View.GONE
        binding.cardFooter.visibility = View.VISIBLE
        binding.modifyTask.visibility = View.GONE
        binding.addFileLayout.visibility = View.VISIBLE
        if (group?.type == 1) binding.layoutCreateObtain.visibility = View.GONE
        else binding.layoutCreateObtain.visibility = View.VISIBLE
    }

    private fun showTaskUI(task: Task, group: Group?, isOwner: Boolean) {
        binding.cardContent.visibility = View.VISIBLE
        binding.taskTitle.text = task.title
        binding.memberName.text = task.nameCreatedBy
        binding.taskDeadline.text = when (task.typeDeadline){
            0 -> {
                if (!task.dateOnly.containsKey(userId)) task.deadline
                else task.dateOnly[userId]
            }
            1 -> task.deadline
            else -> task.deadline
        }
        binding.cardCreate.visibility = View.GONE
        binding.edtTaskName.setText(task.title)
        binding.edtTaskDescription.setText(task.description)
        when (task.typeDeadline) {
            0 -> {
                binding.txtOption.text = binding.root.context.getString(R.string.select_amount_of_time)
                binding.layoutAmountTime.visibility = View.VISIBLE
            }
            1 -> {
                binding.txtOption.text = binding.root.context.getString(R.string.select_day_hour)
                binding.layoutPickDate.visibility = View.VISIBLE
                binding.btnHour.text = task.hourOnly
                binding.btnHour.visibility = View.VISIBLE
                binding.btnDate.text = when (group!!.type) {
                    1 -> {
                        task.dateOnly[userId]
                    }
                    else -> {
                        task.dateOnly[task.assignedTo]
                    }
                }
                binding.btnDate.visibility = View.VISIBLE
            }
            2 -> {
                binding.txtOption.text = binding.root.context.getString(R.string.select_day)
                binding.layoutPickDate.visibility = View.VISIBLE
                binding.btnHour.visibility = View.GONE
                binding.btnDate.text = when (group!!.type) {
                    1 -> {
                        task.dateOnly[userId]
                    }
                    else -> {
                        task.dateOnly[task.assignedTo]
                    }
                }
                binding.btnDate.visibility = View.VISIBLE
            }
            else -> {
                Unit
            }
        }

        //display dựa theo group type
        if (group!!.type == 1){
            binding.layoutObtain.visibility = View.GONE
            binding.taskState.visibility = View.VISIBLE
            if (userId == task.createdBy || isOwner) binding.modifyTask.visibility = View.VISIBLE
            binding.taskState.text = when (task.status[userId!!]) {
                0 -> binding.root.context.getString(R.string.to_do)
                1 -> binding.root.context.getString(R.string.in_progress)
                3 -> binding.root.context.getString(R.string.done)
                else -> {
                    binding.root.context.getString(R.string.canceled)
                }
            }
        }else{
            binding.layoutObtain.visibility = View.VISIBLE
            binding.obtainName.text = task.nameAssignedTo
            binding.assigneeName.text = task.nameAssignedTo
            if (userId == task.assignedTo) {
                binding.addFileLayout.visibility = View.VISIBLE
                binding.taskState.visibility = View.VISIBLE
                binding.modifyTask.visibility = View.GONE
                binding.taskState.text = when (task.status[userId]) {
                    0 -> binding.root.context.getString(R.string.to_do)
                    1 -> binding.root.context.getString(R.string.in_progress)
                    2 -> binding.root.context.getString(R.string.review)
                    3 -> binding.root.context.getString(R.string.done)
                    else -> {
                        binding.root.context.getString(R.string.canceled)
                    }
                }
            }else {
                binding.taskState.visibility = View.GONE
                binding.modifyTask.visibility = View.VISIBLE
                binding.cardFooter.visibility = View.GONE
            }
        }
        if (task.status[userId] == 3) binding.modifyTask.visibility = View.GONE
        binding.addFileLayout.visibility = View.GONE
        updateWithStatus(task, task.status[userId!!]!!)
    }

    fun updateWithStatus(task: Task?, status: Int) {
        when (status) {
            0 -> {
                binding.btnAction.text = binding.root.context.getString(R.string.start)
                binding.btnAction.setTextColor(binding.root.context.getColor(R.color.blue))
                binding.taskState.text = binding.root.context.getString(R.string.to_do)
                binding.taskState.backgroundTintList = ColorStateList.valueOf(binding.root.context.getColor(
                    R.color.blue))
                binding.btnCancel.visibility = View.GONE
            }
            1 -> {
                binding.btnAction.text = binding.root.context.getString(R.string.done)
                binding.btnAction.setTextColor(binding.root.context.getColor(R.color.green))
                binding.taskState.text = binding.root.context.getString(R.string.in_progress)
                binding.taskState.backgroundTintList = ColorStateList.valueOf(binding.root.context.getColor(
                    R.color.yellow))
                binding.btnCancel.visibility = View.VISIBLE
            }
            2 -> {
                if (userId == task!!.assignedTo) binding.btnAction.visibility = View.GONE
                else {
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.text = binding.root.context.getString(R.string.accept)
                    binding.btnAction.setTextColor(binding.root.context.getColor(R.color.blue))
                }
                binding.taskState.text = binding.root.context.getString(R.string.review)
                binding.taskState.backgroundTintList = ColorStateList.valueOf(binding.root.context.getColor(
                    R.color.yellow))
                binding.btnCancel.visibility = View.GONE
            }
            3 -> {
                binding.btnAction.visibility = View.GONE
                binding.taskState.text = binding.root.context.getString(R.string.done)
                binding.taskState.backgroundTintList = ColorStateList.valueOf(binding.root.context.getColor(
                    R.color.green))
                binding.btnCancel.visibility = View.GONE
            }
            else -> {
                binding.btnAction.text = binding.root.context.getString(R.string.start)
                binding.btnAction.setTextColor(binding.root.context.getColor(R.color.blue))
                binding.taskState.text = binding.root.context.getString(R.string.canceled)
                binding.taskState.backgroundTintList = ColorStateList.valueOf(binding.root.context.getColor(
                    R.color.gray))
                binding.btnCancel.visibility = View.GONE
            }
        }
    }

    fun editTask() {
        binding.txtError.visibility = View.GONE
        binding.modifyTask.visibility = View.GONE
        binding.txtDone.visibility = View.VISIBLE
        binding.txtCancel.visibility = View.VISIBLE
        binding.cardContent.visibility = View.GONE
        binding.btnBack.visibility = View.GONE
        binding.taskTitle.visibility = View.GONE
        binding.taskState.visibility = View.GONE
        binding.cardCreate.visibility = View.VISIBLE
        binding.addFileLayout.visibility = View.VISIBLE
    }

    fun doneEditTask() {
        binding.modifyTask.visibility = View.VISIBLE
        binding.txtDone.visibility = View.GONE
        binding.txtCancel.visibility = View.GONE
        binding.btnBack.visibility = View.VISIBLE
        binding.taskTitle.visibility = View.VISIBLE
        binding.taskState.visibility = View.VISIBLE
        binding.cardContent.visibility = View.VISIBLE
        binding.cardCreate.visibility = View.GONE
        binding.addFileLayout.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun selectDay() {
        binding.layoutAmountTime.visibility = View.GONE
        binding.layoutPickDate.visibility = View.VISIBLE
        binding.btnHour.visibility = View.GONE
        binding.btnDate.visibility = View.VISIBLE
        binding.layoutPickHour.visibility = View.GONE
        binding.datePicker.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun selectDayHour() {
        binding.layoutAmountTime.visibility = View.GONE
        binding.layoutPickDate.visibility = View.VISIBLE
        binding.btnHour.visibility = View.VISIBLE
        binding.btnDate.visibility = View.VISIBLE
        binding.layoutPickHour.visibility = View.GONE
        binding.datePicker.visibility = View.GONE
    }
}