defmodule Token do
  @enforce_keys [:type, :lexeme, :line]
  defstruct [:type, :lexeme, :literal, :line]

  def keywords do
    MapSet.new(
      [
        :and,
        :break,
        :class,
        :else,
        false,
        :fun,
        :for,
        :if,
        :in,
        nil,
        :or,
        :print,
        :return,
        :super,
        :this,
        true,
        :var,
        :while
      ]
      |> Enum.map(&Atom.to_string/1)
    )
  end
end
