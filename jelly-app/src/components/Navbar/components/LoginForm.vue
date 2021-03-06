<template>
  <el-form :model="loginForm" :rules="rules" ref="loginForm" class="pl-2 pr-2">
    <div class="type-tab">
      <span class="tab-item active" @click="passwordLogin">密码登录</span>
      <span class="tab-item" @click="messageLogin">短信登录</span>
    </div>
    <div v-if="loginType === 'PASSWORD'">
      <el-form-item prop="account">
        <el-input v-model="loginForm.account" placeholder="邮箱 / 手机号" />
      </el-form-item>
      <el-form-item  prop="password">
        <el-input type="password" v-model="loginForm.password" @keyup.enter.native="submitForm" placeholder="请输入密码"/>
      </el-form-item>  
    </div>
    <div v-if="loginType === 'MESSAGE'">
      <el-form-item prop="telephone">
        <el-input v-model="loginForm.telephone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item prop="smsCode">
        <el-input v-model="loginForm.smsCode" @keyup.enter.native="submitForm" placeholder="请获取验证码">
          <el-button id="verifyBtn" slot="append" type="primary" style="width: 115px;" @click="getSmsCode">
            {{verifyHint}}
          </el-button>
        </el-input>
      </el-form-item>
    </div>
    <div>
      <el-checkbox v-model="remember" >
        <span style="font-size: 13px;">记住密码</span>
        <small class="pl-2 text-black-50">请注意勾选</small>
      </el-checkbox>
      <router-link to="/forget_password" target="_blank" class="float-right">忘记密码?</router-link>
    </div>
    <div class="login-btn pb-3">
      <el-button type="primary" @click="submitForm">登录</el-button>
      <el-button class="float-right" @click="goRegister">注册</el-button>
    </div>
  </el-form>
</template>

<script lang="ts">
import { Component, Vue, Ref, Emit } from 'vue-property-decorator';
import { Action } from 'vuex-class';
import { Form } from 'element-ui';

@Component
export default class LoginForm extends Vue {
  @Ref('loginForm') private refLoginForm!: Form
  @Action('account/login') private login: any
  @Action('app/closeLoginDialog') private closeDialog: any
  
  // data
  private loginType = 'PASSWORD';
  private remember = true;
  private verifyHint = '获取验证码';
  private loginForm = {
    account: '',
    password: '',
    telephone: '',
    smsCode: '',
  };
  private rules = {
    account: [
      { required: true, message: '请输入账号', trigger: 'change' },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'change' },
    ],
    telephone: [
      { required: true, message: '请输入手机号', trigger: 'change' },
    ],
    smsCode: [
      { required: true, message: '请输入验证码', trigger: 'change' },
    ],
  };
  // method
  private submitForm() {
    this.refLoginForm.validate((valid) => {
      if (valid) {
        this.$store.dispatch('account/login', {account: this.loginForm.account, password: this.loginForm.password})
        .then((res: any) => {
          this.$message({type: 'success', message: '登录成功'});
          this.closeDialog();
          this.refLoginForm.resetFields();
          this.$store.dispatch('account/currentUser')
        })
      } else {
        this.$log.info('登录', 'error submit!!');
        return false;
      }
    });
  }
  private passwordLogin(e: any) {
    const $dom = $(e.target);
    $dom.siblings().removeClass('active');
    $dom.addClass('active');
    this.loginType = 'PASSWORD';
    this.refLoginForm.resetFields();
  }
  private messageLogin(e: any) {
    const $dom = $(e.target);
    $dom.siblings().removeClass('active');
    $dom.addClass('active');
    this.loginType = 'MESSAGE';
    this.refLoginForm.resetFields();
  }
  private getSmsCode(e: any) {
    $('#verifyBtn').prop('disabled', true);  
    let second = 10;
    const interval = setInterval(() => {
      second--;
      this.verifyHint = second + 's重新获取';
      if (second <= 0) {
        window.clearInterval(interval);
        this.verifyHint = '获取验证码';
        $('#verifyBtn').prop('disabled', false);
      }
    }, 1000);
  }
  // 事件
  @Emit('switch')
  private goRegister() {
    // ...
  }
}
</script>

<style scoped lang="scss">
.type-tab {
  margin-bottom: 10px;
  .tab-item {
    cursor: pointer;
    padding-right: 1rem;
  }
  .active {
    color: #02a7de;
  }
}
</style>
