import flask
import werkzeug
import time

from flask import send_from_directory

app = flask.Flask(__name__)
app.config["3DFILES"] = "C:/Users/EndlessRiver/Desktop/21/ARScene/FlaskServer/3DMODELS"
#模型文件夹绝对路径

@app.route('/', methods = ['GET', 'POST'])
def handle_request():
    files_ids = list(flask.request.files)
    #文件列表
    print("\n接收文件数：", len(files_ids))
    file_num = 1
    for file_id in files_ids:
        print("\n保存文件： ", str(file_num), "/", len(files_ids))
        videofile = flask.request.files[file_id]
        filename = werkzeug.utils.secure_filename(videofile.filename)
        #生成文件名
        print("\n已上传文件： " + videofile.filename)
        timestr = time.strftime("%Y%m%d-%H%M%S")
        videofile.save(timestr+'_'+filename)
        #保存文件
        file_num = file_num + 1
    print("\n")
    return "文件上传成功，稍后访问“http://主机IP:5000/get-models/视频名称.fbx”以获取模型文件！ "
    #返回客户端信息

def modelDownload(model_name):
    #异常处理
    if '/' in model_name:
        return 'error' , 400
    if model_name:
        return send_file(f"{FILE_FOLDER}/{model_name}", attachment_filename='model.stl')
    return 'error' , 400

@app.route("/get-models/<model_name>")
def get_models (model_name):
    try:
        return send_from_directory(
            app.config["3DFILES"], filename = model_name, as_attachment=True
            #生成下载页
            )
    except FileNotFoundError:
        abort(404)

app.run(host="0.0.0.0", port=5000, debug=True)
