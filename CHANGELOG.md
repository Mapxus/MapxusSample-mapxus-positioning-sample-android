# Positioning SDK Change Log

Version 0.3.4

* 去掉权限`Manifest.permission.WRITE_EXTERNAL_STORAGE`

### Version 0.3.3

* 去掉权限`Manifest.permission.READ_PHONE_STATE`
* 使用UUID

### Version 0.3.2

* 增加检测手机是否支持Gnss的工具类 

### version 0.3.1

* 定位过程中出现网络中断会继续定位，不退出定位服务

### Version 0.3.0

* 新增支持室外定位功能（仅限支持Gnss的手机能使用），室外定位返回空的建筑和楼层信息
* 新增 `WARNING` 错误类型，用以提示用户而不会退出定位服务
* 实现定位过程中切换定位楼层的接口
* 更新各接口的名字及包名地址
* 更换正式包的仓库地址，可以使用jcenter自动集成
* 解决上传包pom文件缺失问题
