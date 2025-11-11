package com.xmobile.project1groupstudyappnew.utils

import android.util.Patterns

object ValidateUtil {
     fun conditionCheckPassword(password: String): Boolean {
          return password.length >= 6
     }

     fun conditionCheckMatching(password: String, confirmPassword: String): Boolean {
          return password == confirmPassword
     }

     fun conditionCheckUserName(userName: String): Boolean {
          return userName.length >= 6
     }

     fun conditionCheckGroupName(name: String): Boolean {
          return name.length >= 20
     }

     fun conditionCheckDescription(description: String): Boolean {
          return description.length >= 100
     }

     fun formatCheck(email: String): Boolean {
          return Patterns.EMAIL_ADDRESS.matcher(email).matches()
     }

     fun emptyCheckUserName(userName: String): Boolean {
          return userName.isNotEmpty()
     }

     fun emptyCheckEmail(email: String): Boolean {
          return email.isNotEmpty()
     }

     fun emptyCheckPassword(password: String): Boolean {
          return password.isNotEmpty()
     }

     fun emptyCheckGroupName(name: String): Boolean {
          return name.isNotEmpty()
     }

     fun emptyCheckGroupId(groupId: String): Boolean {
          return groupId.isNotEmpty()
     }

     fun emptyCheckFileName(name: String): Boolean {
          return name.isNotEmpty()
     }

     fun emptyCheckTaskName(name: String): Boolean {
          return name.isNotEmpty()
     }

     fun emptyCheckDescriptionTask(description: String): Boolean {
          return description.isNotEmpty()
     }

     fun emptyCheckQuantity(quantity: Int): Boolean {
          return quantity > 0
     }

     fun emptyCheckType(type: Int): Boolean {
          return type > 0
     }

     fun conditionCheckDate(state: Int): Boolean { //check future date, true = 1
          return state == 1
     }

     fun emptyAssignedTo(assignedTo: String): Boolean {
          return assignedTo.isNotEmpty()
     }
}
