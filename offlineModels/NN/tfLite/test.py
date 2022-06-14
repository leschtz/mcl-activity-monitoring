import numpy as np
import tensorflow as tf

print(tf.__version__)
path = '../tfLite/tfLiteModelConverted/model.tflite'

# Load the TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(path)
interpreter.allocate_tensors()
signatures = interpreter.get_signature_list()
print(signatures)
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()
print(input_details)
print(output_details)

# Test the model on random input data.
input_shape = input_details[0]['shape']
input_data = np.array(np.random.random_sample(input_shape), dtype=np.float32)
interpreter.set_tensor(input_details[0]['index'], input_data)

interpreter.invoke()

# The function `get_tensor()` returns a copy of the tensor data.
# Use `tensor()` in order to get a pointer to the tensor.
output_data = interpreter.get_tensor(output_details[0]['index'])
print('test', output_data)
infer = interpreter.get_signature_runner("infer")
train = interpreter.get_signature_runner("train")

testvector = np.ones(15,dtype=np.float32)
out = infer(feature=testvector)
print(out)
input = tf.constant(testvector, dtype=tf.float32)
inputlabel  = tf.constant([[1,0,0,0,0,0]], dtype=tf.float32)
label = np.array([1,0,0,0,0,0],dtype=np.float32)
label.transpose()
loss = train(bottleneck=testvector, label=label)

out = infer(feature=input)
print(out)
testvector = np.zeros(15)
input = tf.constant(testvector, dtype=tf.float32)
output = None
out = infer(feature=input)
print(out)



path = '../tfLite/test/model.tflite'
interpreter = tf.lite.Interpreter(path)
interpreter.allocate_tensors()
signatures = interpreter.get_signature_list()
print(signatures)
# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()
print(input_details)
print(output_details)

# Test the model on random input data.
input_shape = input_details[0]['shape']
input_data = np.array(np.random.random_sample(input_shape), dtype=np.float32)
interpreter.set_tensor(input_details[0]['index'], input_data)

interpreter.invoke()

# The function `get_tensor()` returns a copy of the tensor data.
# Use `tensor()` in order to get a pointer to the tensor.
output_data = interpreter.get_tensor(output_details[0]['index'])
print('test', output_data)
