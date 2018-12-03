# Positioning SDK Change Log

### Version 0.3.13

* 角度信息添加角度的精度等级，建议在非高精度模式下使用倾斜8字旋转来校准角度
* 添加、修改错误信息返回

### Version 0.3.12

* 修复部分机型没有更换新算法的bug

### Version 0.3.11

* 更换使用的定位算法

### Version 0.3.10

* 修复定位算法中的bug
* 定位出现网络错误时再多次重试

### Version 0.3.9

* 修复室内外切换出错、楼层切换速度慢且不准确问题
* 修复多线程安全漏洞，优化定位流程


### Version 0.3.8

* 修改精度返回接口名称


### Version 0.3.7

* 定位信息增加定位精度返回，单位：米
* 缩短初始定位时间
* 修复多线程安全漏洞，并优化定位流程
* 修改错误的返回，未开启定位服务或开启但不是高精度模式时会报`ERROR_LOCATION_SERVICE_DISABLED`错误

注意：使用0.3.7版本及以上如果需要同时使用MapxusMap，MapxusMap SDK须升级为**2.4.1**及以上版本

### Version 0.3.6

* 更新新版授权包，旧的id和secret后续会失效，请尽快联系获取新版的id与secret
* 优化定位算法
* 无气压室内定位只在单楼层定位，楼层切换需调用changeFloor接口

注意：使用0.3.6版本如果需要同时使用到MapxusMap，MapxusMap SDK版本须设置为**2.3.3-beta**

### Version 0.3.5

* 支持没有气压传感器的设备使用室内定位服务

### Version 0.3.4

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
