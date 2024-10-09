defmodule Token do
  @enforce_keys [:type, :lexeme, :line]
  defstruct [:type, :lexeme, :literal, :line]
end
