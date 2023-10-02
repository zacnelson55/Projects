"""
Author: Sophia Sanborn, Sagnik Bhattacharya
Institution: UC Berkeley
Date: Spring 2020
Course: CS189/289A
Website: github.com/sophiaas, github.com/sagnibak
"""

import numpy as np
from abc import ABC, abstractmethod

from neural_networks.activations import initialize_activation
from neural_networks.weights import initialize_weights
from collections import OrderedDict

from typing import Callable, List, Literal, Tuple, Union


class Layer(ABC):
    """Abstract class defining the `Layer` interface."""

    def __init__(self):
        self.activation = None

        self.n_in = None
        self.n_out = None

        self.parameters = {}
        self.cache = {}
        self.gradients = {}

        super().__init__()

    @abstractmethod
    def forward(self, z: np.ndarray) -> np.ndarray:
        pass

    def clear_gradients(self) -> None:
        self.cache = OrderedDict({a: [] for a, b in self.cache.items()})
        self.gradients = OrderedDict(
            {a: np.zeros_like(b) for a, b in self.gradients.items()}
        )

    def forward_with_param(
        self, param_name: str, X: np.ndarray,
    ) -> Callable[[np.ndarray], np.ndarray]:
        """Call the `forward` method but with `param_name` as the variable with
        value `param_val`, and keep `X` fixed.
        """

        def inner_forward(param_val: np.ndarray) -> np.ndarray:
            self.parameters[param_name] = param_val
            return self.forward(X)

        return inner_forward

    def _get_parameters(self) -> List[np.ndarray]:
        return [b for a, b in self.parameters.items()]

    def _get_cache(self) -> List[np.ndarray]:
        return [b for a, b in self.cache.items()]

    def _get_gradients(self) -> List[np.ndarray]:
        return [b for a, b in self.gradients.items()]


def initialize_layer(
    name: str,
    activation: str = None,
    weight_init: str = None,
    n_out: int = None,
    kernel_shape: Tuple[int, int] = None,
    stride: int = None,
    pad: int = None,
    mode: str = None,
    keep_dim: str = "first",
) -> Layer:
    """Factory function for layers."""
    if name == "fully_connected":
        return FullyConnected(
            n_out=n_out, activation=activation, weight_init=weight_init,
        )

    elif name == "conv2d":
        return Conv2D(
            n_out=n_out,
            activation=activation,
            kernel_shape=kernel_shape,
            stride=stride,
            pad=pad,
            weight_init=weight_init,
        )

    elif name == "pool2d":
        return Pool2D(kernel_shape=kernel_shape, mode=mode, stride=stride, pad=pad)

    elif name == "flatten":
        return Flatten(keep_dim=keep_dim)

    else:
        raise NotImplementedError("Layer type {} is not implemented".format(name))


class FullyConnected(Layer):
    """A fully-connected layer multiplies its input by a weight matrix, adds
    a bias, and then applies an activation function.
    """

    def __init__(
        self, n_out: int, activation: str, weight_init="xavier_uniform"
    ) -> None:

        super().__init__()
        self.n_in = None
        self.n_out = n_out
        self.activation = initialize_activation(activation)

        # instantiate the weight initializer
        self.init_weights = initialize_weights(weight_init, activation=activation)

    def _init_parameters(self, X_shape: Tuple[int, int]) -> None:
        """Initialize all layer parameters (weights, biases)."""
        self.n_in = X_shape[1]

        ### BEGIN YOUR CODE ###

        W = self.init_weights((self.n_in, self.n_out))
        b = np.zeros((1, self.n_out))

        self.parameters = OrderedDict({"W": W, "b": b})
        self.cache = OrderedDict({"Z": [], "X": []})   # cache for backprop
        self.gradients = OrderedDict({"W": np.zeros(W.shape), "b": np.zeros(b.shape)})  # parameter gradients initialized to zero
                                           # MUST HAVE THE SAME KEYS AS `self.parameters`

        ### END YOUR CODE ###

    def forward(self, X: np.ndarray) -> np.ndarray:
        """Forward pass: multiply by a weight matrix, add a bias, apply activation.
        Also, store all necessary intermediate results in the `cache` dictionary
        to be able to compute the backward pass.

        Parameters
        ----------
        X  input matrix of shape (batch_size, input_dim)

        Returns
        -------
        a matrix of shape (batch_size, output_dim)
        """
        # initialize layer parameters if they have not been initialized
        if self.n_in is None:
            self._init_parameters(X.shape)

        ### BEGIN YOUR CODE ###
        
        # perform an affine transformation and activation
        Z = X @ self.parameters["W"] + self.parameters["b"]
        out = self.activation(Z)
        self.cache["Z"] = Z
        self.cache["X"] = X
        
        # store information necessary for backprop in `self.cache`

        ### END YOUR CODE ###

        return out

    def backward(self, dLdY: np.ndarray) -> np.ndarray:
        """Backward pass for fully connected layer.
        Compute the gradients of the loss with respect to:
            1. the weights of this layer (mutate the `gradients` dictionary)
            2. the bias of this layer (mutate the `gradients` dictionary)
            3. the input of this layer (return this)

        Parameters
        ----------
        dLdY  derivative of the loss with respect to the output of this layer
              shape (batch_size, output_dim)

        Returns
        -------
        derivative of the loss with respect to the input of this layer
        shape (batch_size, input_dim)
        """
        ### BEGIN YOUR CODE ###
        
        # unpack the cache
        
        # compute the gradients of the loss w.r.t. all parameters as well as the
        # input of the layer
        W = self.parameters["W"]
        b = self.parameters["b"]
        Z = self.cache["Z"]
        X = self.cache["X"]

        dZ = self.activation.backward(Z, dLdY)
        dW = X.T @ dZ
        db = sum(dZ)
        dX = dZ @ W.T


        # store the gradients in `self.gradients`
        # the gradient for self.parameters["W"] should be stored in
        # self.gradients["W"], etc.

        self.gradients["W"] = dW
        self.gradients["b"] = db

        ### END YOUR CODE ###

        return dX


class Conv2D(Layer):
    """Convolutional layer for inputs with 2 spatial dimensions."""

    def __init__(
        self,
        n_out: int,
        kernel_shape: Tuple[int, int],
        activation: str,
        stride: int = 1,
        pad: str = "same",
        weight_init: str = "xavier_uniform",
    ) -> None:

        super().__init__()
        self.n_in = None
        self.n_out = n_out
        self.kernel_shape = kernel_shape
        self.stride = stride
        self.pad = pad

        self.activation = initialize_activation(activation)
        self.init_weights = initialize_weights(weight_init, activation=activation)

    def _init_parameters(self, X_shape: Tuple[int, int, int, int]) -> None:
        """Initialize all layer parameters and determine padding."""
        self.n_in = X_shape[3]

        W_shape = self.kernel_shape + (self.n_in,) + (self.n_out,)
        W = self.init_weights(W_shape)
        b = np.zeros((1, self.n_out))

        self.parameters = OrderedDict({"W": W, "b": b})
        self.cache = OrderedDict({"Z": [], "X": []})
        self.gradients = OrderedDict({"W": np.zeros_like(W), "b": np.zeros_like(b)})

        if self.pad == "same":
            self.pad = ((W_shape[0] - 1) // 2, (W_shape[1] - 1) // 2)
        elif self.pad == "valid":
            self.pad = (0, 0)
        elif isinstance(self.pad, int):
            self.pad = (self.pad, self.pad)
        else:
            raise ValueError("Invalid Pad mode found in self.pad.")

    def forward(self, X: np.ndarray) -> np.ndarray:
        """Forward pass for convolutional layer. This layer convolves the input
        `X` with a filter of weights, adds a bias term, and applies an activation
        function to compute the output. This layer also supports padding and
        integer strides. Intermediates necessary for the backward pass are stored
        in the cache.

        Parameters
        ----------
        X  input with shape (batch_size, in_rows, in_cols, in_channels)

        Returns
        -------
        output feature maps with shape (batch_size, out_rows, out_cols, out_channels)
        """
        if self.n_in is None:
            self._init_parameters(X.shape)

        W = self.parameters["W"]
        b = self.parameters["b"]

        kernel_height, kernel_width, in_channels, out_channels = W.shape
        n_examples, in_rows, in_cols, in_channels = X.shape
        kernel_shape = (kernel_height, kernel_width)

        ### BEGIN YOUR CODE ###

        # implement a convolutional forward pass
        pad0 = self.pad[0]
        pad1 = self.pad[1]

        pad_width = ((0, 0), (pad0, pad0), (pad1, pad1), (0, 0))
        pad = np.pad(X, pad_width, mode='constant')

        out_rows = in_rows + 2*pad0
        out_rows -= kernel_height
        out_rows = int(out_rows / self.stride)
        out_rows += 1

        out_cols = in_cols + 2*pad1
        out_cols -= kernel_width
        out_cols = int(out_cols / self.stride)
        out_cols += 1

        Z = np.zeros((n_examples, out_rows, out_cols, out_channels))
        shape = Z.shape

        for i in range(shape[1]):
            for j in range(shape[2]):
                for c in range(shape[3]):
                    w = W[:, :, :, c]
                    x = pad[:, self.stride*i:self.stride*i + kernel_height, 
                            self.stride*j:self.stride*j + kernel_width, :]
                    Z[:, i, j, c] = np.sum(w*x, axis=(1,2,3)) + b[:, c]
        
        out = self.activation(Z)

        # cache any values required for backprop
        self.cache["X"] = X
        self.cache["Z"] = Z
        ### END YOUR CODE ###

        return out

    def backward(self, dLdY: np.ndarray) -> np.ndarray:
        """Backward pass for conv layer. Computes the gradients of the output
        with respect to the input feature maps as well as the filter weights and
        biases.

        Parameters
        ----------
        dLdY  derivative of loss with respect to output of this layer
              shape (batch_size, out_rows, out_cols, out_channels)

        Returns
        -------
        derivative of the loss with respect to the input of this layer
        shape (batch_size, in_rows, in_cols, in_channels)
        """
        ### BEGIN YOUR CODE ###
        W = self.parameters["W"]
        b = self.parameters["b"]
        X = self.cache["X"]
        Z = self.cache["Z"]

        kernel_height, kernel_width, in_channels, out_channels = W.shape
        n_examples, in_rows, in_cols, in_channels = X.shape
        kernel_shape = (kernel_height, kernel_width)

        pad0 = self.pad[0]
        pad1 = self.pad[1]


        dZ = self.activation.backward(Z, dLdY)
        dW = np.zeros(W.shape)
        dX = np.zeros(pad.shape)


        out_rows = in_rows + 2*pad0
        out_rows -= kernel_height
        out_rows = int(out_rows / self.stride)
        out_rows += 1

        out_cols = in_cols + 2*pad1
        out_cols -= kernel_width
        out_cols = int(out_cols / self.stride)
        out_cols += 1

        # perform a backward pass
        shape = Z.shape

        for i in range(shape[1]):
            for j in range(shape[2]):
                for c in range(shape[3]):
                    dW[..., c] += sum(dZ[:, i:i+1, j:j+1, np.newaxis, c] * 
                                      pad[:, self.stride*i:self.stride*i+kernel_height,
                                          self.stride*j:self.stride*j+kernel_width, :])
                    dX[:, self.stride*i:self.stride*i+kernel_height,
                       self.stride*j:self.stride*j+kernel_width, :
                       ] += dZ[:, i:i+1, j:j+1, np.newaxis, c] * W[..., c]

        dX = dX[:, self.pad[0]:self.pad[0]+in_rows, self.pad[1]:self.pad[1]+in_cols, :]
        self.gradients["W"] = dW
        db = np.sum(dZ, axis=(1,2,3), keepdims=True)
        self.gradients["b"] = db
        ### END YOUR CODE ###

        return dX

class Pool2D(Layer):
    """Pooling layer, implements max and average pooling."""

    def __init__(
        self,
        kernel_shape: Tuple[int, int],
        mode: str = "max",
        stride: int = 1,
        pad: Union[int, Literal["same"], Literal["valid"]] = 0,
    ) -> None:

        if type(kernel_shape) == int:
            kernel_shape = (kernel_shape, kernel_shape)

        self.kernel_shape = kernel_shape
        self.stride = stride

        if pad == "same":
            self.pad = ((kernel_shape[0] - 1) // 2, (kernel_shape[1] - 1) // 2)
        elif pad == "valid":
            self.pad = (0, 0)
        elif isinstance(pad, int):
            self.pad = (pad, pad)
        else:
            raise ValueError("Invalid Pad mode found in self.pad.")

        self.mode = mode

        if mode == "max":
            self.pool_fn = np.max
            self.arg_pool_fn = np.argmax
        elif mode == "average":
            self.pool_fn = np.mean

        self.cache = {
            "out_rows": [],
            "out_cols": [],
            "X_pad": [],
            "p": [],
            "pool_shape": [],
        }
        self.parameters = {}
        self.gradients = {}

    def forward(self, X: np.ndarray) -> np.ndarray:
        """Forward pass: use the pooling function to aggregate local information
        in the input. This layer typically reduces the spatial dimensionality of
        the input while keeping the number of feature maps the same.

        As with all other layers, please make sure to cache the appropriate
        information for the backward pass.

        Parameters
        ----------
        X  input array of shape (batch_size, in_rows, in_cols, channels)

        Returns
        -------
        pooled array of shape (batch_size, out_rows, out_cols, channels)
        """
        ### BEGIN YOUR CODE ###

        # implement the forward pass
        kernel_height, kernel_width = self.kernel_shape
        n_examples, in_rows, in_cols, in_channels = X.shape
        kernel_shape = (kernel_height, kernel_width)

        pad0 = self.pad[0]
        pad1 = self.pad[1]
        
        out_rows = in_rows + 2*pad0
        out_rows -= kernel_height
        out_rows = int(out_rows / self.stride)
        out_rows += 1

        out_cols = in_cols + 2*pad1
        out_cols -= kernel_width
        out_cols = int(out_cols / self.stride)
        out_cols += 1

        X_pool = np.zeros((n_examples, out_rows, out_cols, in_channels))

        pad_width = ((0, 0), (pad0, pad0), (pad1, pad1), (0, 0))
        pad = np.pad(X, pad_width, mode='constant')


        for i in range(out_rows):
            for j in range(out_cols):
                X_pool[:, i, j, :] = self.pool_fn(pad[:, self.stride*i: self.stride*i + kernel_height,
                                           self.stride*j: self.stride*j + kernel_width], axis=(1,2))


        # cache any values required for backprop
        self.cache["X"] = X
        ### END YOUR CODE ###

        return X_pool

    def backward(self, dLdY: np.ndarray) -> np.ndarray:
        """Backward pass for pooling layer.

        Parameters
        ----------
        dLdY  gradient of loss with respect to the output of this layer
              shape (batch_size, out_rows, out_cols, channels)

        Returns
        -------
        gradient of loss with respect to the input of this layer
        shape (batch_size, in_rows, in_cols, channels)
        """
        ### BEGIN YOUR CODE ###

        # perform a backward pass
        X = self.cache["X"]

        kernel_height, kernel_width = self.kernel_shape
        n_examples, in_rows, in_cols, in_channels = X.shape

        pad0 = self.pad[0]
        pad1 = self.pad[1]
        
        out_rows = in_rows + 2*pad0
        out_rows -= kernel_height
        out_rows = int(out_rows / self.stride)
        out_rows += 1

        out_cols = in_cols + 2*pad1
        out_cols -= kernel_width
        out_cols = int(out_cols / self.stride)
        out_cols += 1

        pad_width = ((0, 0), (pad0, pad0), (pad1, pad1), (0, 0))
        pad = np.pad(X, pad_width, mode='constant')

        dX = np.zeros(pad.shape)

        for i in range(out_rows):
            for j in range(out_cols):
                if self.pool_fn == np.max:
                    x = pad[:, self.stride*i: self.stride*i + kernel_height,
                                           self.stride*j: self.stride*j + kernel_width, :]
                    x = x.reshape(n_examples, -1, in_channels)
                    kernel = np.zeros(x.shape)
                    a, c = np.indices((n_examples, in_channels))
                    kernel[a, np.argmax(x, axis=1), c] = 1
                    kernel = kernel.reshape(n_examples, kernel_height,kernel_width, in_channels)
                    dX[:, self.stride*i: self.stride*i + kernel_height,
                       self.stride*j: self.stride*j + kernel_width, :
                       ] += dLdY[:, i:i+1, j:j+1, :] * kernel
                else:
                    dX[:, self.stride*i: self.stride*i + kernel_height,
                       self.stride*j: self.stride*j + kernel_width, :
                       ] += dLdY[:, i:i+1, j:j+1, :] / (kernel_height * kernel_width)
        
        dX = dX[:, pad0:pad0+in_rows, pad1:pad1+in_cols, :]
        ### END YOUR CODE ###

        return dX

class Flatten(Layer):
    """Flatten the input array."""

    def __init__(self, keep_dim: str = "first") -> None:
        super().__init__()

        self.keep_dim = keep_dim
        self._init_params()

    def _init_params(self):
        self.X = []
        self.gradients = {}
        self.parameters = {}
        self.cache = {"in_dims": []}

    def forward(self, X: np.ndarray, retain_derived: bool = True) -> np.ndarray:
        self.cache["in_dims"] = X.shape

        if self.keep_dim == -1:
            return X.flatten().reshape(1, -1)

        rs = (X.shape[0], -1) if self.keep_dim == "first" else (-1, X.shape[-1])
        return X.reshape(*rs)

    def backward(self, dLdY: np.ndarray) -> np.ndarray:
        in_dims = self.cache["in_dims"]
        dX = dLdY.reshape(in_dims)
        return dX
